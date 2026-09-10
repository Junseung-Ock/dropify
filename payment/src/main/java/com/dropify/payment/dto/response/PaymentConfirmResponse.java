package com.dropify.payment.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentConfirmResponse {
    private final Long orderId;
    private final String orderStatus;
    private final Long amount;
    private final LocalDateTime paidAt;

    public PaymentConfirmResponse(Long orderId, String orderStatus, Long amount, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
        this.amount = amount;
        this.paidAt = paidAt;
    }
}
