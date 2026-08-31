package com.dropify.usecase;

import com.dropify.order.service.OrderService;
import com.dropify.order.service.PaymentService;
import com.dropify.order.usecase.CancelOrderUseCase;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl implements CancelOrderUseCase {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;

    @Override
    @Transactional
    public void cancel(Long userId, Long orderId) {
        paymentService.cancelOrder(userId, orderId);
        rollbackStock(orderId);
    }

    @Override
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
