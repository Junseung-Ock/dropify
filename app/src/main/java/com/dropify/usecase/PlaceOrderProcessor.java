package com.dropify.usecase;

import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.lock.DistributedLock;
import com.dropify.order.service.OrderCreationService;
import com.dropify.order.service.PaymentService;
import com.dropify.product.domain.entity.Product;
import com.dropify.product.service.ProductService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PlaceOrderProcessor {

    private final ProductService productService;
    private final OrderCreationService orderCreationService;
    private final StockService stockService;
    private final PaymentService paymentService;

    @DistributedLock(key = "'order:lock:' + #request.productId")
    @Transactional
    PlaceOrderResponse process(Long userId, PlaceOrderRequest request) {
        Product product = productService.getEntityById(request.getProductId());

        PlaceOrderResponse response = orderCreationService.create(
                userId, product.getId(), request.getQuantity(), product.getPrice());

        stockService.decreaseStock(product.getId(), request.getQuantity());
        paymentService.createPendingPayment(response.getOrderId(), response.getTotalAmount());

        return response;
    }
}
