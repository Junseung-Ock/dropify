package com.dropify.order.usecase;

import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;

public interface PlaceOrderUseCase {
    PlaceOrderResponse execute(Long userId, PlaceOrderRequest request, String idempotencyKey);
}
