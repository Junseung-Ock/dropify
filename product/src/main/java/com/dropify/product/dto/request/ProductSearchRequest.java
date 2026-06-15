package com.dropify.product.dto.request;

import com.dropify.product.domain.entity.ProductStatus;
import lombok.Getter;

@Getter
public class ProductSearchRequest {

    private String keyword;
    private ProductStatus status;
    private Long minPrice;
    private Long maxPrice;
}
