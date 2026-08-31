package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.response.OrderDetailResponse;
import com.dropify.order.dto.response.OrderSummaryResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

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
    // getOrderItems
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrderItems")
    class GetOrderItems {

        @Test
        @DisplayName("주문이 존재하면 해당 주문의 아이템 목록을 반환한다")
        void getOrderItems_orderExists_returnsItems() {
            OrderItem item = mock(OrderItem.class);
            Order order = mock(Order.class);
            when(order.getOrderItems()).thenReturn(List.of(item));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            List<OrderItem> result = orderService.getOrderItems(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("주문이 존재하지 않으면 빈 리스트를 반환한다")
        void getOrderItems_orderNotFound_returnsEmptyList() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            List<OrderItem> result = orderService.getOrderItems(999L);

            assertThat(result).isEmpty();
        }
    }
}
