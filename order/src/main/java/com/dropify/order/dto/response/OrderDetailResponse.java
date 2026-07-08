package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderDetailResponse {

    private final Long orderId;
    private final OrderStatus status;
    private final Long totalAmount;
    private final LocalDateTime createdAt;
    private final List<OrderItemResponse> items;

    public OrderDetailResponse(Order order) {
        this.orderId = order.getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.createdAt = order.getCreatedAt();
        this.items = order.getOrderItems().stream()
                .map(OrderItemResponse::new)
                .toList();
    }

    @Getter
    public static class OrderItemResponse {

        private final Long productId;
        private final int quantity;
        private final Long unitPrice;
        private final Long totalPrice;

        public OrderItemResponse(OrderItem item) {
            this.productId = item.getProductId();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.totalPrice = item.getTotalPrice();
        }
    }
}
