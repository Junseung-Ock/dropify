package com.dropify.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceOrderRequest {

    @Schema(example = "1")
    @NotNull
    private Long productId;

    @Schema(example = "2")
    @Positive
    private int quantity;
}
