package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.dto.request.StockReplenishRequest;
import com.dropify.product.dto.response.StockReplenishResponse;
import com.dropify.product.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    private final StockService stockService;

    @PostMapping("/{productId}/stock")
    public ApiResponse<StockReplenishResponse> replenish(
            @PathVariable Long productId,
            @RequestBody @Valid StockReplenishRequest request) {
        return ApiResponse.ok(stockService.replenishStock(productId, request));
    }
}
