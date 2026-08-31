package com.dropify.web.usecase;

import com.dropify.order.service.OrderService;
import com.dropify.payment.service.PaymentService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;

    @Transactional
    public void cancel(Long userId, Long orderId) {
        paymentService.cancelOrder(userId, orderId);
        rollbackStock(orderId);
    }

    @Transactional
    public void cancelByUser(Long userId, Long orderId) {
        boolean cancelled = paymentService.cancelByUser(userId, orderId);
        if (cancelled) {
            rollbackStock(orderId);
        }
    }

    private void rollbackStock(Long orderId) {
        orderService.getOrderItems(orderId).forEach(item ->
                stockService.rollbackStock(item.getProductId(), item.getQuantity()));
    }
}
