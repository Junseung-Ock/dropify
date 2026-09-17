package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.dto.request.ProductSearchRequest;
import com.dropify.product.dto.response.ProductResponse;
import com.dropify.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product", description = "상품")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 단건 조회", security = {})
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)")
    )
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @Operation(summary = "상품 목록 검색", security = {})
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "minPrice > maxPrice (COMMON_001)")
    )
    @GetMapping
    public ApiResponse<Page<ProductResponse>> search(
            @Validated ProductSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(productService.search(request, pageable));
    }
}
