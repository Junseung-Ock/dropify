package com.dropify.product.dto.request;

import com.dropify.product.domain.entity.ProductStatus;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchRequest {

    private String keyword;
    private ProductStatus status;
    private Long minPrice;
    private Long maxPrice;

    @AssertTrue(message = "minPrice는 maxPrice보다 클 수 없습니다.")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice <= maxPrice;
    }
}
