package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
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
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;
    private final StockService stockService;

    // Toss API 실패 시에도 payment.fail() + order.cancel() + 재고 롤백이 커밋되어야 하므로 noRollbackFor 설정
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
            return new PaymentConfirmResponse(order, payment);
        } catch (BusinessException e) {
            if (payment.fail()) {
                order.cancel();
                rollbackStock(order);
            }
            log.warn("결제 실패 처리 완료: orderId={}", order.getId());
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
                rollbackStock(order);
            }
        } else if (order.getStatus() == OrderStatus.PAID) {
            tossPaymentClient.cancel(payment.getTossPaymentKey(), "사용자 취소");
            if (payment.cancel()) {
                order.cancel();
                rollbackStock(order);
            }
        } else {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        log.info("주문 취소 완료: orderId={}, status={}", orderId, order.getStatus());
    }

    @Transactional
    public void cancelByUser(Long userId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        if (payment.fail()) {
            order.cancel();
            rollbackStock(order);
            log.info("결제창 취소 처리 완료: orderId={}", orderId);
        }
    }

    @Transactional
    public void handleWebhook(TossWebhookEvent event) {
        if (!tossPaymentProperties.getWebhookSecret().equals(event.getSecret())) {
            log.warn("웹훅 시크릿 불일치: 무시");
            return;
        }

        if (!"PAYMENT_STATUS_CHANGED".equals(event.getType())) {
            return;
        }

        Long orderId;
        try {
            String rawOrderId = event.getOrderId().replace("order-", "");
            orderId = Long.parseLong(rawOrderId);
        } catch (NumberFormatException e) {
            log.warn("웹훅 orderId 파싱 실패: {}", event.getOrderId());
            return;
        }

        Payment payment = paymentRepository.findByOrderIdWithLock(orderId).orElse(null);
        if (payment == null) return;

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        String status = event.getStatus();

        if ("DONE".equals(status) && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.complete(event.getPaymentKey())) {
                order.markAsPaid();
                log.info("웹훅 결제 완료 처리: orderId={}", orderId);
            }
        } else if (("ABORTED".equals(status) || "EXPIRED".equals(status) || "CANCELED".equals(status))
                && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.fail()) {
                order.cancel();
                rollbackStock(order);
                log.warn("웹훅 결제 실패 처리: orderId={}, status={}", orderId, status);
            }
        } else if ("CANCELED".equals(status) && payment.getStatus() == PaymentStatus.COMPLETED) {
            if (payment.cancel()) {
                order.cancel();
                rollbackStock(order);
                log.warn("웹훅 외부 결제 취소 처리: orderId={}, status={}", orderId, status);
            }
        }
    }

    private void rollbackStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            stockService.rollbackStock(item.getProductId(), item.getQuantity());
        }
    }
}
