package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.lock.DistributedLock;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    /**
     * 분산 락 범위 안에서 DB 재고 최종 검증 → 차감 → 주문 생성을 처리한다.
     * Redis 사전 확인을 통과한 요청만 여기에 도달하므로,
     * DB 재고 차감 시 BusinessException(INSUFFICIENT_STOCK) 발생은 극히 드문 경쟁 조건에서만 발생한다.
     */
    @DistributedLock(key = "'order:lock:' + #request.productId")
    @Transactional
    public PlaceOrderResponse create(Long userId, PlaceOrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(product.getPrice() * request.getQuantity())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .quantity(request.getQuantity())
                .unitPrice(product.getPrice())
                .build();

        order.addOrderItem(item);
        orderRepository.save(order);

        // DB 재고 검증 + 차감 (product.decreaseStock이 부족 시 INSUFFICIENT_STOCK 예외 발생)
        stockService.decreaseStock(product.getId(), request.getQuantity());

        return new PlaceOrderResponse(order);
    }
}
