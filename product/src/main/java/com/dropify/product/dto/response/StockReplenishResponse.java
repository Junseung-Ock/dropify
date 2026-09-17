package com.dropify.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockReplenishResponse {

    @Schema(example = "1")
    private Long productId;

    @Schema(example = "150")
    private int currentStock;
}
