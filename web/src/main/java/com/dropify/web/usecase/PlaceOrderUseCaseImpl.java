package com.dropify.web.usecase;

import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.IdempotencyService;
import com.dropify.product.service.StockService;
import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCaseImpl {

    private final PlaceOrderProcessor processor;
    private final IdempotencyService idempotencyService;
    private final StockService stockService;

    public PlaceOrderResponse execute(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        Optional<PlaceOrderResponse> cached = idempotencyService.get(userId, idempotencyKey);
        if (cached.isPresent()) return cached.get();

        if (!idempotencyService.reserve(userId, idempotencyKey)) {
            return idempotencyService.get(userId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_ORDER));
        }

        try {
            stockService.checkRedisStock(request.getProductId(), request.getQuantity());
            PlaceOrderResponse response = processor.process(userId, request);
            idempotencyService.complete(userId, idempotencyKey, response);
            return response;
        } catch (RuntimeException e) {
            idempotencyService.release(userId, idempotencyKey);
            throw e;
        }
    }
}
