package com.dropify.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockReplenishRequest {

    @NotNull
    @Min(value = 1, message = "보충 수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @Size(max = 255, message = "사유는 255자 이하여야 합니다.")
    private String reason;
}
