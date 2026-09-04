package com.dropify.payment.dto.response;

import com.dropify.payment.domain.entity.PaymentHistory;
import com.dropify.payment.domain.entity.PaymentHistoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentHistoryResponse {

    private Long orderId;
    private Long amount;
    private LocalDateTime paidAt;
    private PaymentHistoryStatus status;

    public static PaymentHistoryResponse from(PaymentHistory history) {
        return PaymentHistoryResponse.builder()
                .orderId(history.getOrderId())
                .amount(history.getAmount())
                .paidAt(history.getPaidAt())
                .status(history.getStatus())
                .build();
    }
}
