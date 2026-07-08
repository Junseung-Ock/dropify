package com.dropify.order.concurrency;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 분산 락 없이 OrderCreationService와 동일한 로직 — 데이터 오염 비교용 테스트 전용
@Service
public class NoLockOrderCreationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public NoLockOrderCreationService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

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

        product.decreaseStock(request.getQuantity());

        return new PlaceOrderResponse(order);
    }
}
