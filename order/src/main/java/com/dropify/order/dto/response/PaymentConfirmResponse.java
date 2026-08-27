package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.payment.domain.entity.Payment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentConfirmResponse {
    private final Long orderId;
    private final OrderStatus orderStatus;
    private final Long amount;
    private final LocalDateTime paidAt;

    public PaymentConfirmResponse(Order order, Payment payment) {
        this.orderId = order.getId();
        this.orderStatus = order.getStatus();
        this.amount = payment.getAmount();
        this.paidAt = payment.getPaidAt();
    }
}
