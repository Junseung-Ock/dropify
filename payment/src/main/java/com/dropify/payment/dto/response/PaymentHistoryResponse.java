package com.dropify.payment.dto.response;

import com.dropify.payment.domain.entity.PaymentHistory;
import com.dropify.payment.domain.entity.PaymentHistoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentHistoryResponse {

    @Schema(example = "1")
    private Long orderId;

    @Schema(example = "29800")
    private Long amount;

    @Schema(example = "2024-01-01T10:00:00")
    private LocalDateTime paidAt;

    @Schema(example = "PAID")
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
