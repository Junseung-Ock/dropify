package com.dropify.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductCreateRequest {

    @Schema(example = "Dropify 티셔츠")
    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @Schema(example = "편안한 일상 티셔츠")
    private String description;

    @Schema(example = "29800")
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Long price;

    @Schema(example = "100")
    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stockQuantity;
}
