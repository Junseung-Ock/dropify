package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STOCK_KEY = "stock:";

    private final OrderCreationService orderCreationService;
    private final IdempotencyService idempotencyService;
    private final OrderEventPublisher orderEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public PlaceOrderResponse placeOrder(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        // ① 멱등성 키 확인 — 이미 처리된 요청이면 캐시된 응답 반환
        return idempotencyService.get(userId, idempotencyKey)
                .orElseGet(() -> processOrder(userId, request, idempotencyKey));
    }

    private PlaceOrderResponse processOrder(Long userId, PlaceOrderRequest request, String idempotencyKey) {
        // ② Redis 재고 사전 확인 — 명백히 재고 없는 요청 조기 차단
        checkRedisStock(request.getProductId(), request.getQuantity());

        // ③ 락 획득 → DB 재고 최종 검증 → 차감 → 주문 PENDING 생성 → 락 해제
        PlaceOrderResponse response = orderCreationService.create(userId, request);

        // ④ 멱등성 키에 결과 캐시 (24시간) — Kafka 실패 시 재시도가 중복 주문 생성하지 않도록 먼저 저장
        idempotencyService.save(userId, idempotencyKey, response);

        // ⑤ 락 해제 후 payment-request 이벤트 발행
        orderEventPublisher.publishPaymentRequest(userId, response);

        return response;
    }

    private void checkRedisStock(Long productId, int quantity) {
        String stock = redisTemplate.opsForValue().get(STOCK_KEY + productId);
        if (stock != null && Integer.parseInt(stock) < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }
}
