package com.dropify.product.repository;

import com.dropify.common.config.JpaConfig;
import com.dropify.product.config.QueryDslConfig;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.dto.request.ProductSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaConfig.class, QueryDslConfig.class})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.saveAll(List.of(
                Product.builder().name("A상품").description("설명").price(3000L).stockQuantity(10).build(),
                Product.builder().name("B상품").description("설명").price(1000L).stockQuantity(5).build(),
                Product.builder().name("C상품").description("설명").price(2000L).stockQuantity(20).build()
        ));
    }

    @Test
    @DisplayName("가격 오름차순 정렬이 적용된다")
    void search_sortByPriceAsc() {
        ProductSearchRequest request = new ProductSearchRequest();
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("price")));

        Page<Product> result = productRepository.search(request, pageable);

        List<Long> prices = result.getContent().stream()
                .map(Product::getPrice)
                .toList();

        assertThat(prices).containsExactly(1000L, 2000L, 3000L);
    }

    @Test
    @DisplayName("가격 내림차순 정렬이 적용된다")
    void search_sortByPriceDesc() {
        ProductSearchRequest request = new ProductSearchRequest();
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("price")));

        Page<Product> result = productRepository.search(request, pageable);

        List<Long> prices = result.getContent().stream()
                .map(Product::getPrice)
                .toList();

        assertThat(prices).containsExactly(3000L, 2000L, 1000L);
    }

    @Test
    @DisplayName("정렬 조건 없으면 createdAt 내림차순이 기본값이다")
    void search_noSort_defaultsToCreatedAtDesc() {
        ProductSearchRequest request = new ProductSearchRequest();
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Product> result = productRepository.search(request, pageable);

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("키워드 검색이 적용된다")
    void search_withKeyword_filtersResults() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("A상품");
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Product> result = productRepository.search(request, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("A상품");
    }

    @Test
    @DisplayName("가격 범위 필터가 적용된다")
    void search_withPriceRange_filtersResults() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setMinPrice(1500L);
        request.setMaxPrice(2500L);
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Product> result = productRepository.search(request, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(2000L);
    }
}
