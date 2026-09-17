package com.dropify.order.dto.response;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PlaceOrderResponse {

    @Schema(example = "1")
    private final Long orderId;

    @Schema(example = "PENDING")
    private final OrderStatus status;

    @Schema(example = "29800")
    private final Long totalAmount;

    public PlaceOrderResponse(Order order) {
        this.orderId = order.getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
    }

    @JsonCreator
    public PlaceOrderResponse(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("status") OrderStatus status,
            @JsonProperty("totalAmount") Long totalAmount) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
    }
}
