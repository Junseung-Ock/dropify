package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderSummaryResponse {

    private final Long orderId;
    private final OrderStatus status;
    private final Long totalAmount;
    private final LocalDateTime createdAt;

    public OrderSummaryResponse(Order order) {
        this.orderId = order.getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.createdAt = order.getCreatedAt();
    }
}
