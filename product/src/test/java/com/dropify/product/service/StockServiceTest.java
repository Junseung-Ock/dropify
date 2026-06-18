package com.dropify.product.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.dto.request.StockReplenishRequest;
import com.dropify.product.dto.response.StockReplenishResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("재고 차감 시 DB를 업데이트하고 커밋 후 Redis를 감소시킨다")
    void decreaseStock_success() {
        Product product = mock(Product.class);
        when(product.getStockQuantity()).thenReturn(10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        try (MockedStatic<TransactionSynchronizationManager> tsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
               .thenAnswer(invocation -> {
                   TransactionSynchronization sync = invocation.getArgument(0);
                   sync.afterCommit();
                   return null;
               });

            stockService.decreaseStock(1L, 3);

            verify(product).decreaseStock(3);
            verify(valueOperations).decrement("stock:1", 3L);
        }
    }

    @Test
    @DisplayName("재고 차감 시 상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void decreaseStock_productNotFound_throwsBusinessException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.decreaseStock(999L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("재고 보충 시 DB를 업데이트하고 커밋 후 Redis를 증가시키며 응답을 반환한다")
    void replenishStock_success() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getStockQuantity()).thenReturn(15);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        StockReplenishRequest request = mock(StockReplenishRequest.class);
        when(request.getQuantity()).thenReturn(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        try (MockedStatic<TransactionSynchronizationManager> tsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
               .thenAnswer(invocation -> {
                   TransactionSynchronization sync = invocation.getArgument(0);
                   sync.afterCommit();
                   return null;
               });

            StockReplenishResponse response = stockService.replenishStock(1L, request);

            verify(product).increaseStock(5);
            verify(valueOperations).increment("stock:1", 5L);
            assertThat(response.getProductId()).isEqualTo(1L);
            assertThat(response.getCurrentStock()).isEqualTo(15);
        }
    }

    @Test
    @DisplayName("재고 보충 시 상품이 없으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void replenishStock_productNotFound_throwsBusinessException() {
        StockReplenishRequest request = mock(StockReplenishRequest.class);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.replenishStock(999L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }
}
