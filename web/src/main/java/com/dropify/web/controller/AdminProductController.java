package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.domain.entity.ProductStatus;
import com.dropify.product.dto.request.ProductCreateRequest;
import com.dropify.product.dto.request.ProductUpdateRequest;
import com.dropify.product.dto.response.ProductResponse;
import com.dropify.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Product", description = "관리자 상품 관리")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "상품 등록")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (COMMON_001)")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@RequestBody @Valid ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @Operation(summary = "상품 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (COMMON_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)")
    })
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @Operation(summary = "상품 상태 변경")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)")
    )
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam ProductStatus status) {
        productService.changeStatus(id, status);
        return ApiResponse.ok();
    }

    @Operation(summary = "상품 삭제")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)")
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }
}
