package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.OrderDetailResponse;
import com.dropify.order.dto.response.OrderSummaryResponse;
import com.dropify.order.dto.response.PlaceOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STOCK_KEY = "stock:";

    private final OrderRepository orderRepository;
    private final OrderCreationService orderCreationService;
    private final IdempotencyService idempotencyService;
    private final StringRedisTemplate redisTemplate;

    public PlaceOrderResponse placeOrder(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        // ① 멱등성 키 확인 — 이미 처리된 요청이면 캐시된 응답 반환
        return idempotencyService.get(userId, idempotencyKey)
                .orElseGet(() -> processOrder(userId, request, idempotencyKey));
    }

    private PlaceOrderResponse processOrder(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        // ② Redis 재고 사전 확인 — 명백히 재고 없는 요청 조기 차단
        checkRedisStock(request.getProductId(), request.getQuantity());

        // ③ 락 획득 → DB 재고 최종 검증 → 차감 → 주문/Payment PENDING 생성 → 락 해제
        PlaceOrderResponse pendingResponse = orderCreationService.create(userId, request);

        // ④ 락 해제 후 PG 호출 → 주문/Payment 상태 확정 (별도 트랜잭션)
        PlaceOrderResponse response = orderCreationService.finalizePayment(pendingResponse.getOrderId());

        // ⑤ 멱등성 키에 결과 캐시 (24시간)
        idempotencyService.save(userId, idempotencyKey, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderSummaryResponse::new);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return new OrderDetailResponse(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.cancel();
    }

    private void checkRedisStock(Long productId, int quantity) {
        String stock = redisTemplate.opsForValue().get(STOCK_KEY + productId);
        if (stock != null && Integer.parseInt(stock) < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }
}
