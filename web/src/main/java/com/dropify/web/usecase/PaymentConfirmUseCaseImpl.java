package com.dropify.web.usecase;

import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.dropify.order.service.OrderService;
import com.dropify.payment.dto.request.PaymentConfirmRequest;
import com.dropify.payment.dto.response.PaymentConfirmResponse;
import com.dropify.payment.exception.PaymentConfirmFailedException;
import com.dropify.payment.service.PaymentService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConfirmUseCaseImpl {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(noRollbackFor = PaymentConfirmFailedException.class)
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        orderService.getOrderByIdAndUserId(request.getOrderId(), userId);

        try {
            PaymentConfirmResponse response = paymentService.confirm(userId, request);
            orderService.markAsPaid(request.getOrderId());

            eventPublisher.publishEvent(PaymentCompletedEvent.builder()
                    .orderId(request.getOrderId())
                    .userId(userId)
                    .amount(response.getAmount())
                    .paidAt(response.getPaidAt())
                    .build());

            return response;
        } catch (PaymentConfirmFailedException e) {
            orderService.cancelOrder(request.getOrderId());
            orderService.getOrderItems(request.getOrderId()).forEach(item ->
                    stockService.rollbackStock(item.getProductId(), item.getQuantity()));

            eventPublisher.publishEvent(PaymentFailedEvent.builder()
                    .orderId(request.getOrderId())
                    .userId(userId)
                    .build());
            eventPublisher.publishEvent(OrderCancelledEvent.builder()
                    .orderId(request.getOrderId())
                    .userId(userId)
                    .build());

            throw e;
        }
    }
}
