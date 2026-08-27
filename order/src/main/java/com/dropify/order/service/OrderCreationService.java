package com.dropify.order.service;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderItem;
import com.dropify.order.domain.repository.OrderRepository;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.lock.DistributedLock;
import com.dropify.payment.domain.entity.Payment;
import com.dropify.payment.domain.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;

    // 분산 락 + 트랜잭션 — 재고 차감, 주문/Payment PENDING 생성 후 즉시 커밋·락 해제
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

        stockService.decreaseStock(product.getId(), request.getQuantity());

        paymentRepository.save(Payment.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .build());

        return new PlaceOrderResponse(order);
    }
}
