package com.dropify.order.service;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.lock.DistributedLock;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import com.dropify.product.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    /**
     * 분산 락 범위 안에서 주문 생성 + 재고 차감을 처리한다.
     * key: 'order:lock:{productId}' — 같은 상품에 대한 동시 주문을 직렬화해 재고 초과 판매를 방지한다.
     */
    @DistributedLock(key = "'order:lock:' + #request.productId")
    @Transactional
    public PlaceOrderResponse placeOrder(Long userId, @Valid PlaceOrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        long unitPrice = product.getPrice();

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(unitPrice * request.getQuantity())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .build();

        order.addOrderItem(item);
        orderRepository.save(order);

        stockService.decreaseStock(product.getId(), request.getQuantity());

        return new PlaceOrderResponse(order);
    }
}
