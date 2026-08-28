package com.dropify.usecase;

import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.IdempotencyService;
import com.dropify.order.usecase.PlaceOrderUseCase;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

    private final PlaceOrderProcessor processor;
    private final IdempotencyService idempotencyService;
    private final StockService stockService;

    @Override
    public PlaceOrderResponse execute(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        return idempotencyService.get(userId, idempotencyKey)
                .orElseGet(() -> processOrder(userId, request, idempotencyKey));
    }

    private PlaceOrderResponse processOrder(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        stockService.checkRedisStock(request.getProductId(), request.getQuantity());
        PlaceOrderResponse response = processor.process(userId, request);
        idempotencyService.save(userId, idempotencyKey, response);
        return response;
    }
}
