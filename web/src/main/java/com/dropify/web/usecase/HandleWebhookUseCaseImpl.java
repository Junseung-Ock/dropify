package com.dropify.web.usecase;

import com.dropify.order.dto.request.TossWebhookEvent;
import com.dropify.order.service.OrderService;
import com.dropify.payment.service.PaymentService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandleWebhookUseCaseImpl {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;

    @Transactional
    public void handle(TossWebhookEvent event) {
        paymentService.handleWebhook(event)
                .ifPresent(orderId -> orderService.getOrderItems(orderId).forEach(item ->
                        stockService.rollbackStock(item.getProductId(), item.getQuantity())));
    }
}
