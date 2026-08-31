package com.dropify.payment.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.exception.PaymentConfirmFailedException;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PaymentConfirmRequest;
import com.dropify.order.dto.request.TossWebhookEvent;
import com.dropify.order.dto.response.PaymentConfirmResponse;
import com.dropify.payment.client.TossPaymentClient;
import com.dropify.payment.config.TossPaymentProperties;
import com.dropify.payment.domain.entity.Payment;
import com.dropify.payment.domain.entity.PaymentStatus;
import com.dropify.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Transactional
    public void createPendingPayment(Long orderId, Long amount) {
        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .build());
    }

    // Toss API 실패 시에도 payment.fail() + order.cancel()이 커밋되어야 하므로 noRollbackFor 설정
    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (!payment.getAmount().equals(request.getAmount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        try {
            var tossResponse = tossPaymentClient.confirm(
                    request.getPaymentKey(),
                    "order-" + request.getOrderId(),
                    request.getAmount()
            );
            payment.complete(tossResponse.getPaymentKey());
            order.markAsPaid();
            log.info("결제 승인 완료: orderId={}", order.getId());
            return new PaymentConfirmResponse(order, payment.getAmount(), payment.getPaidAt());
        } catch (BusinessException e) {
            if (payment.fail()) {
                order.cancel();
                log.warn("결제 실패 처리 완료: orderId={}", order.getId());
                throw new PaymentConfirmFailedException(e.getErrorCode());
            }
            throw e;
        }
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (order.getStatus() == OrderStatus.PENDING) {
            if (payment.fail()) {
                order.cancel();
            }
        } else if (order.getStatus() == OrderStatus.PAID) {
            tossPaymentClient.cancel(payment.getTossPaymentKey(), "사용자 취소");
            if (payment.cancel()) {
                order.cancel();
            }
        } else {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        log.info("주문 취소 완료: orderId={}, status={}", orderId, order.getStatus());
    }

    // 결제창 취소 시 호출 — PENDING이 아니면 무시, 실제 취소 여부를 반환
    @Transactional
    public boolean cancelByUser(Long userId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }

        if (payment.fail()) {
            order.cancel();
            log.info("결제창 취소 처리 완료: orderId={}", orderId);
            return true;
        }

        return false;
    }

    // 재고 롤백이 필요한 경우 orderId를 반환, 불필요하면 empty
    @Transactional
    public Optional<Long> handleWebhook(TossWebhookEvent event) {
        if (!tossPaymentProperties.getWebhookSecret().equals(event.getSecret())) {
            log.warn("웹훅 시크릿 불일치: 무시");
            return Optional.empty();
        }

        if (!"PAYMENT_STATUS_CHANGED".equals(event.getType())) {
            return Optional.empty();
        }

        Long orderId;
        try {
            String rawOrderId = event.getOrderId().replace("order-", "");
            orderId = Long.parseLong(rawOrderId);
        } catch (NumberFormatException e) {
            log.warn("웹훅 orderId 파싱 실패: {}", event.getOrderId());
            return Optional.empty();
        }

        Payment payment = paymentRepository.findByOrderIdWithLock(orderId).orElse(null);
        if (payment == null) return Optional.empty();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return Optional.empty();

        String status = event.getStatus();

        if ("DONE".equals(status) && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.complete(event.getPaymentKey())) {
                order.markAsPaid();
                log.info("웹훅 결제 완료 처리: orderId={}", orderId);
            }
            return Optional.empty();
        } else if (("ABORTED".equals(status) || "EXPIRED".equals(status) || "CANCELED".equals(status))
                && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.fail()) {
                order.cancel();
                log.warn("웹훅 결제 실패 처리: orderId={}, status={}", orderId, status);
                return Optional.of(orderId);
            }
        } else if ("CANCELED".equals(status) && payment.getStatus() == PaymentStatus.COMPLETED) {
            if (payment.cancel()) {
                order.cancel();
                log.warn("웹훅 외부 결제 취소 처리: orderId={}, status={}", orderId, status);
                return Optional.of(orderId);
            }
        }

        return Optional.empty();
    }
}
