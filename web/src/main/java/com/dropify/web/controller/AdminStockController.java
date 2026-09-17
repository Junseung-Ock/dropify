package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.dto.request.StockReplenishRequest;
import com.dropify.product.dto.response.StockReplenishResponse;
import com.dropify.product.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Stock", description = "관리자 재고 관리")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    private final StockService stockService;

    @Operation(summary = "재고 보충")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (COMMON_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)")
    })
    @PostMapping("/{productId}/stock")
    public ApiResponse<StockReplenishResponse> replenish(
            @PathVariable Long productId,
            @RequestBody @Valid StockReplenishRequest request) {
        return ApiResponse.ok(stockService.replenishStock(productId, request));
    }
}
