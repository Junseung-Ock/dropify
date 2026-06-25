package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import lombok.Getter;

@Getter
public class PlaceOrderResponse {

    private final Long orderId;
    private final OrderStatus status;
    private final Long totalAmount;

    public PlaceOrderResponse(Order order) {
        this.orderId = order.getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
    }
}
