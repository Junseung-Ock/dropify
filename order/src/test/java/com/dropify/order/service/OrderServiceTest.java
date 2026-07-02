package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.event.OrderEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderCreationService orderCreationService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("멱등성 키 캐시 히트 시 주문 생성 없이 캐시된 응답을 반환한다")
    void placeOrder_idempotencyKeyHit_returnsCachedResponse() {
        PlaceOrderResponse cached = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);
        when(idempotencyService.get(1L, "test-key")).thenReturn(Optional.of(cached));

        PlaceOrderRequest request = mock(PlaceOrderRequest.class);
        PlaceOrderResponse result = orderService.placeOrder(1L, request, "test-key");

        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(orderCreationService, never()).create(any(), any());
        verify(idempotencyService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("멱등성 키 캐시 미스 시 주문을 생성하고 결과를 Redis에 저장한다")
    void placeOrder_idempotencyKeyMiss_createsOrderAndSaves() {
        PlaceOrderResponse response = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);

        PlaceOrderRequest request = mock(PlaceOrderRequest.class);
        when(request.getProductId()).thenReturn(1L);
        when(request.getQuantity()).thenReturn(1);

        when(idempotencyService.get(1L, "test-key")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("stock:1")).thenReturn("100");
        when(orderCreationService.create(1L, request)).thenReturn(response);

        PlaceOrderResponse result = orderService.placeOrder(1L, request, "test-key");

        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(orderCreationService).create(1L, request);
        verify(idempotencyService).save(1L, "test-key", response);
        verify(orderEventPublisher).publishPaymentRequest(1L, response);
    }

    @Test
    @DisplayName("Redis 재고가 부족하면 INSUFFICIENT_STOCK 예외가 발생한다")
    void placeOrder_insufficientStock_throwsBusinessException() {
        PlaceOrderRequest request = mock(PlaceOrderRequest.class);
        when(request.getProductId()).thenReturn(1L);
        when(request.getQuantity()).thenReturn(5);

        when(idempotencyService.get(1L, "test-key")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("stock:1")).thenReturn("2");

        assertThatThrownBy(() -> orderService.placeOrder(1L, request, "test-key"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        verify(orderCreationService, never()).create(any(), any());
    }
}
