package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.OrderDetailResponse;
import com.dropify.order.dto.response.OrderSummaryResponse;
import com.dropify.order.dto.response.PlaceOrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
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
    private OrderRepository orderRepository;

    @Mock
    private OrderCreationService orderCreationService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    // ─────────────────────────────────────────────────────────────────────────
    // placeOrder
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

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
        @DisplayName("멱등성 키 캐시 미스 시 주문을 생성하고 결제 완료 결과를 Redis에 저장한다")
        void placeOrder_idempotencyKeyMiss_createsOrderAndSaves() {
            PlaceOrderResponse pendingResponse = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);
            PlaceOrderResponse paidResponse = new PlaceOrderResponse(1L, OrderStatus.PAID, 10000L);

            PlaceOrderRequest request = mock(PlaceOrderRequest.class);
            when(request.getProductId()).thenReturn(1L);
            when(request.getQuantity()).thenReturn(1);

            when(idempotencyService.get(1L, "test-key")).thenReturn(Optional.empty());
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("stock:1")).thenReturn("100");
            when(orderCreationService.create(1L, request)).thenReturn(pendingResponse);
            when(orderCreationService.finalizePayment(1L)).thenReturn(paidResponse);

            PlaceOrderResponse result = orderService.placeOrder(1L, request, "test-key");

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(orderCreationService).create(1L, request);
            verify(orderCreationService).finalizePayment(1L);
            verify(idempotencyService).save(1L, "test-key", paidResponse);
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

        @Test
        @DisplayName("Redis에 재고 키가 없으면 DB 재고 검증으로 넘어간다")
        void placeOrder_redisStockKeyMissing_proceedsToOrderCreation() {
            PlaceOrderResponse pendingResponse = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);
            PlaceOrderResponse paidResponse = new PlaceOrderResponse(1L, OrderStatus.PAID, 10000L);

            PlaceOrderRequest request = mock(PlaceOrderRequest.class);
            when(request.getProductId()).thenReturn(1L);
            when(request.getQuantity()).thenReturn(3);

            when(idempotencyService.get(1L, "test-key")).thenReturn(Optional.empty());
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("stock:1")).thenReturn(null); // 키 없음
            when(orderCreationService.create(1L, request)).thenReturn(pendingResponse);
            when(orderCreationService.finalizePayment(1L)).thenReturn(paidResponse);

            PlaceOrderResponse result = orderService.placeOrder(1L, request, "test-key");

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(orderCreationService).create(1L, request);
            verify(orderCreationService).finalizePayment(1L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMyOrders
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyOrders")
    class GetMyOrders {

        @Test
        @DisplayName("내 주문 목록 조회 시 페이징된 요약 응답을 반환한다")
        void getMyOrders_success_returnsPaginatedSummary() {
            Order order = mock(Order.class);
            when(order.getId()).thenReturn(1L);
            when(order.getStatus()).thenReturn(OrderStatus.PENDING);
            when(order.getTotalAmount()).thenReturn(10000L);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);

            Page<OrderSummaryResponse> result = orderService.getMyOrders(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
        }

        @Test
        @DisplayName("주문이 없으면 빈 페이지를 반환한다")
        void getMyOrders_noOrders_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<OrderSummaryResponse> result = orderService.getMyOrders(1L, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrderDetail
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetail {

        @Test
        @DisplayName("본인 주문 상세 조회 시 올바른 응답을 반환한다")
        void getOrderDetail_orderExists_returnsDetailResponse() {
            Order order = mock(Order.class);
            when(order.getId()).thenReturn(1L);
            when(order.getStatus()).thenReturn(OrderStatus.PENDING);
            when(order.getTotalAmount()).thenReturn(10000L);
            when(order.getOrderItems()).thenReturn(Collections.emptyList());
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

            OrderDetailResponse result = orderService.getOrderDetail(1L, 1L);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getTotalAmount()).isEqualTo(10000L);
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 주문 또는 타인 주문 조회 시 ORDER_NOT_FOUND 예외가 발생한다")
        void getOrderDetail_orderNotFound_throwsBusinessException() {
            when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelOrder
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 상태 주문 취소 시 엔티티의 cancel()이 호출된다")
        void cancelOrder_pendingOrder_callsCancelOnEntity() {
            Order order = mock(Order.class);
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

            orderService.cancelOrder(1L, 1L);

            verify(order).cancel();
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 ORDER_NOT_FOUND 예외가 발생한다")
        void cancelOrder_orderNotFound_throwsBusinessException() {
            when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("이미 취소된 주문 재취소 시 ORDER_ALREADY_CANCELLED 예외가 발생한다")
        void cancelOrder_alreadyCancelled_throwsBusinessException() {
            Order order = mock(Order.class);
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));
            doThrow(new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED)).when(order).cancel();

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ORDER_ALREADY_CANCELLED.getMessage());
        }

        @Test
        @DisplayName("PENDING이 아닌 주문 취소 시 ORDER_NOT_CANCELLABLE 예외가 발생한다")
        void cancelOrder_paidOrder_throwsBusinessException() {
            Order order = mock(Order.class);
            when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));
            doThrow(new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE)).when(order).cancel();

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.ORDER_NOT_CANCELLABLE.getMessage());
        }
    }
}
