package com.dropify.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotNull
    private Long productId;

    @Positive
    private int quantity;
}
