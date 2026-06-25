package com.dropify.order.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceOrderRequest {

    private Long productId;
    private int quantity;
    private Long unitPrice;
}
