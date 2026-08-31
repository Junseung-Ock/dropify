package com.dropify.order.service;

import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.response.PlaceOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;

    @Transactional
    public PlaceOrderResponse create(Long userId, Long productId, int quantity, Long unitPrice) {
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(unitPrice * quantity)
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(productId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();

        order.addOrderItem(item);
        orderRepository.save(order);

        return new PlaceOrderResponse(order);
    }
}
