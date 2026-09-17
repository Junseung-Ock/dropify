package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderDetailResponse {

    @Schema(example = "1")
    private final Long orderId;

    @Schema(example = "PAID")
    private final OrderStatus status;

    @Schema(example = "29800")
    private final Long totalAmount;

    @Schema(example = "2024-01-01T10:00:00")
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

        @Schema(example = "1")
        private final Long productId;

        @Schema(example = "2")
        private final int quantity;

        @Schema(example = "14900")
        private final Long unitPrice;

        @Schema(example = "29800")
        private final Long totalPrice;

        public OrderItemResponse(OrderItem item) {
            this.productId = item.getProductId();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.totalPrice = item.getTotalPrice();
        }
    }
}
