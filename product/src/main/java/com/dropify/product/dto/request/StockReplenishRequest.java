package com.dropify.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockReplenishRequest {

    @NotNull
    @Min(value = 1, message = "보충 수량은 1 이상이어야 합니다.")
    private Integer quantity;

    private String reason;
}
