package com.dropify.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductUpdateRequest {

    @Schema(example = "Dropify 티셔츠 (리뉴얼)")
    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @Schema(example = "리뉴얼된 일상 티셔츠")
    private String description;

    @Schema(example = "32000")
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Long price;

    @Schema(example = "80")
    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stockQuantity;
}
