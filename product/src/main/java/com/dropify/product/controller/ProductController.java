package com.dropify.product.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.domain.entity.ProductStatus;
import com.dropify.product.dto.request.ProductCreateRequest;
import com.dropify.product.dto.request.ProductSearchRequest;
import com.dropify.product.dto.request.ProductUpdateRequest;
import com.dropify.product.dto.response.ProductResponse;
import com.dropify.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> create(@RequestBody @Valid ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> search(
            ProductSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(productService.search(request, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam ProductStatus status) {
        productService.changeStatus(id, status);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
