package com.dropify.web.usecase;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.service.OrderService;
import com.dropify.payment.domain.WebhookAction;
import com.dropify.payment.domain.WebhookResult;
import com.dropify.payment.dto.request.TossWebhookEvent;
import com.dropify.payment.service.PaymentService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleWebhookUseCaseImpl {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handle(TossWebhookEvent event) {
        WebhookResult result = paymentService.handleWebhook(event);

        if (result.action() == WebhookAction.IGNORED) {
            return;
        }

        Order order = orderService.findOrderById(result.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Long orderId = result.orderId();
        Long userId = order.getUserId();

        switch (result.action()) {
            case COMPLETED -> {
                orderService.markAsPaid(orderId);
                eventPublisher.publishEvent(PaymentCompletedEvent.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .amount(result.amount())
                        .paidAt(result.paidAt())
                        .build());
            }
            case PAYMENT_FAILED -> {
                orderService.cancelOrder(orderId);
                rollbackStock(orderId);
                eventPublisher.publishEvent(PaymentFailedEvent.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .build());
                eventPublisher.publishEvent(OrderCancelledEvent.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .build());
            }
            case EXTERNAL_CANCELLED -> {
                orderService.cancelOrder(orderId);
                rollbackStock(orderId);
                eventPublisher.publishEvent(PaymentCancelledEvent.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .build());
                eventPublisher.publishEvent(OrderCancelledEvent.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .build());
            }
        }
    }

    private void rollbackStock(Long orderId) {
        orderService.getOrderItems(orderId).forEach(item ->
                stockService.rollbackStock(item.getProductId(), item.getQuantity()));
    }
}
