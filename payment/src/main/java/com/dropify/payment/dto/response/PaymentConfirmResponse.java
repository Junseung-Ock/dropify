package com.dropify.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentConfirmResponse {

    @Schema(example = "1")
    private final Long orderId;

    @Schema(example = "PAID")
    private final String orderStatus;

    @Schema(example = "29800")
    private final Long amount;

    @Schema(example = "2024-01-01T10:00:00")
    private final LocalDateTime paidAt;

    public PaymentConfirmResponse(Long orderId, String orderStatus, Long amount, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
        this.amount = amount;
        this.paidAt = paidAt;
    }
}
