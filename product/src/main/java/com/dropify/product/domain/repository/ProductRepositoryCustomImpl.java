package com.dropify.product.domain.repository;

import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.entity.QProduct;
import com.dropify.product.dto.request.ProductSearchRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> search(ProductSearchRequest request, Pageable pageable) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(request.getKeyword())) {
            builder.and(product.name.containsIgnoreCase(request.getKeyword()));
        }
        if (request.getStatus() != null) {
            builder.and(product.status.eq(request.getStatus()));
        }
        if (request.getMinPrice() != null) { // goe = greater or equal
            builder.and(product.price.goe(request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) { // less or equal
            builder.and(product.price.loe(request.getMaxPrice()));
        }

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(product.createdAt.desc())
                .fetch();

        long total = Optional.ofNullable(queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne())
                .orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }
}
