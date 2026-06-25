package com.dropify.order.service;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.lock.DistributedLock;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockService stockService;

    /**
     * 분산 락 범위 안에서 주문 생성 + 재고 차감을 처리한다.
     * key: 'order:lock:{productId}' — 같은 상품에 대한 동시 주문을 직렬화해 재고 초과 판매를 방지한다.
     */
    @DistributedLock(key = "'order:lock:' + #request.productId")
    @Transactional
    public PlaceOrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(request.getUnitPrice() * request.getQuantity())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .build();

        order.addOrderItem(item);
        orderRepository.save(order);

        stockService.decreaseStock(request.getProductId(), request.getQuantity());

        return new PlaceOrderResponse(order);
    }
}
