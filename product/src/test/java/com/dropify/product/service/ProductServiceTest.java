package com.dropify.product.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Test
    @DisplayName("존재하지 않는 상품 삭제 시 BusinessException 발생")
    void delete_productNotFound_throwsBusinessException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("상품 삭제 시 delete(product) 호출 확인")
    void delete_productExists_callsDelete() {
        Product product = mock(Product.class);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
        verify(productRepository, never()).deleteById(any());
    }
}
