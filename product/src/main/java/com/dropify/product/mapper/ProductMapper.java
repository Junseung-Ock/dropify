package com.dropify.product.mapper;

import com.dropify.product.domain.entity.Product;
import com.dropify.product.dto.response.ProductResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);
}
