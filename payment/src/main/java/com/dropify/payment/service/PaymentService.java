package com.dropify.payment.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.payment.client.TossPaymentClient;
import com.dropify.payment.config.TossPaymentProperties;
import com.dropify.payment.domain.WebhookAction;
import com.dropify.payment.domain.WebhookResult;
import com.dropify.payment.domain.entity.Payment;
import com.dropify.payment.domain.entity.PaymentStatus;
import com.dropify.payment.domain.repository.PaymentRepository;
import com.dropify.payment.dto.request.PaymentConfirmRequest;
import com.dropify.payment.dto.request.TossWebhookEvent;
import com.dropify.payment.dto.response.PaymentConfirmResponse;
import com.dropify.payment.exception.PaymentConfirmFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Transactional
    public void createPendingPayment(Long orderId, Long amount) {
        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .build());
    }

    // Toss API 실패 시에도 payment.fail()이 커밋되어야 하므로 noRollbackFor 설정
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

        try {
            var tossResponse = tossPaymentClient.confirm(
                    request.getPaymentKey(),
                    "order-" + request.getOrderId(),
                    request.getAmount()
            );
            payment.complete(tossResponse.getPaymentKey());
            log.info("결제 승인 완료: orderId={}", request.getOrderId());
            return new PaymentConfirmResponse(
                    request.getOrderId(), "PAID", payment.getAmount(), payment.getPaidAt());
        } catch (BusinessException e) {
            if (payment.fail()) {
                log.warn("결제 실패 처리 완료: orderId={}", request.getOrderId());
                throw new PaymentConfirmFailedException(e.getErrorCode());
            }
            throw e;
        }
    }

    @Transactional
    public void failPendingPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.fail();
        log.info("PENDING 결제 실패 처리: orderId={}", orderId);
    }

    @Transactional
    public void cancelPaidPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        tossPaymentClient.cancel(payment.getTossPaymentKey(), "사용자 취소");
        payment.cancel();
        log.info("결제 취소 완료: orderId={}", orderId);
    }

    // 결제창 취소 시 호출 — PENDING이 아니면 무시, 실제 취소 여부를 반환
    @Transactional
    public boolean tryFailPendingPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }

        if (payment.fail()) {
            log.info("결제창 취소 처리 완료: orderId={}", orderId);
            return true;
        }

        return false;
    }

    @Transactional
    public WebhookResult handleWebhook(TossWebhookEvent event) {
        if (!tossPaymentProperties.getWebhookSecret().equals(event.getSecret())) {
            log.warn("웹훅 시크릿 불일치: 무시");
            return WebhookResult.ignored();
        }

        if (!"PAYMENT_STATUS_CHANGED".equals(event.getType())) {
            return WebhookResult.ignored();
        }

        Long orderId;
        try {
            String rawOrderId = event.getOrderId().replace("order-", "");
            orderId = Long.parseLong(rawOrderId);
        } catch (NumberFormatException e) {
            log.warn("웹훅 orderId 파싱 실패: {}", event.getOrderId());
            return WebhookResult.ignored();
        }

        Payment payment = paymentRepository.findByOrderIdWithLock(orderId).orElse(null);
        if (payment == null) return WebhookResult.ignored();

        String status = event.getStatus();

        if ("DONE".equals(status) && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.complete(event.getPaymentKey())) {
                log.info("웹훅 결제 완료 처리: orderId={}", orderId);
                return WebhookResult.completed(orderId, payment.getAmount(), payment.getPaidAt());
            }
        } else if (("ABORTED".equals(status) || "EXPIRED".equals(status) || "CANCELED".equals(status))
                && payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.fail()) {
                log.warn("웹훅 결제 실패 처리: orderId={}, status={}", orderId, status);
                return WebhookResult.paymentFailed(orderId);
            }
        } else if ("CANCELED".equals(status) && payment.getStatus() == PaymentStatus.COMPLETED) {
            if (payment.cancel()) {
                log.warn("웹훅 외부 결제 취소 처리: orderId={}, status={}", orderId, status);
                return WebhookResult.externalCancelled(orderId);
            }
        }

        return WebhookResult.ignored();
    }
}
