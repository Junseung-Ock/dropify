package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderSummaryResponse {

    @Schema(example = "1")
    private final Long orderId;

    @Schema(example = "PAID")
    private final OrderStatus status;

    @Schema(example = "29800")
    private final Long totalAmount;

    @Schema(example = "2024-01-01T10:00:00")
    private final LocalDateTime createdAt;

    public OrderSummaryResponse(Order order) {
        this.orderId = order.getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.createdAt = order.getCreatedAt();
    }
}
