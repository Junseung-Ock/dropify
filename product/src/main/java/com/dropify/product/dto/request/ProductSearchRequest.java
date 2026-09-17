package com.dropify.product.dto.request;

import com.dropify.product.domain.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchRequest {

    @Schema(example = "티셔츠")
    private String keyword;

    @Schema(example = "ON_SALE")
    private ProductStatus status;

    @Schema(example = "10000")
    private Long minPrice;

    @Schema(example = "50000")
    private Long maxPrice;

    @AssertTrue(message = "minPrice는 maxPrice보다 클 수 없습니다.")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice <= maxPrice;
    }
}
