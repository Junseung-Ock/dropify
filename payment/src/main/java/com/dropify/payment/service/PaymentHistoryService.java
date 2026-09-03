package com.dropify.payment.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.payment.domain.entity.PaymentHistory;
import com.dropify.payment.domain.repository.PaymentHistoryRepository;
import com.dropify.payment.dto.response.PaymentHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional
    public void save(Long userId, Long orderId, Long amount, LocalDateTime paidAt) {
        paymentHistoryRepository.save(PaymentHistory.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .paidAt(paidAt)
                .build());
        log.info("결제 내역 저장: userId={}, orderId={}", userId, orderId);
    }

    @Transactional
    public void cancel(Long orderId) {
        PaymentHistory history = paymentHistoryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_HISTORY_NOT_FOUND));
        history.cancel();
        log.info("결제 내역 취소: orderId={}", orderId);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getHistories(Long userId) {
        return paymentHistoryRepository.findByUserIdOrderByPaidAtDesc(userId)
                .stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }
}
