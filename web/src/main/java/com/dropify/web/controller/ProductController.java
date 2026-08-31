package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.product.dto.request.ProductSearchRequest;
import com.dropify.product.dto.response.ProductResponse;
import com.dropify.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> search(
            @Validated ProductSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(productService.search(request, pageable));
    }
}
