package com.dropify.product.dto.response;

import com.dropify.product.domain.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Dropify 티셔츠")
    private String name;

    @Schema(example = "편안한 일상 티셔츠")
    private String description;

    @Schema(example = "29800")
    private Long price;

    @Schema(example = "100")
    private int stockQuantity;

    @Schema(example = "ON_SALE")
    private ProductStatus status;

    @Schema(example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
