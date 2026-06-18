package com.dropify.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockReplenishResponse {
    private Long productId;
    private int currentStock;
}
