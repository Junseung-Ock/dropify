package com.dropify.product.domain.repository;

import com.dropify.product.domain.entity.Product;
import com.dropify.product.dto.request.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> search(ProductSearchRequest request, Pageable pageable);
}
