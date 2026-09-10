package com.dropify.web.usecase;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.order.domain.entity.Order;
import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.service.OrderService;
import com.dropify.payment.service.PaymentService;
import com.dropify.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = orderService.getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() == OrderStatus.PENDING) {
            paymentService.failPendingPayment(orderId);
        } else if (order.getStatus() == OrderStatus.PAID) {
            paymentService.cancelPaidPayment(orderId);
            eventPublisher.publishEvent(PaymentCancelledEvent.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .build());
        } else {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }

        orderService.cancelOrder(orderId);
        rollbackStock(orderId);

        eventPublisher.publishEvent(OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .build());
    }

    @Transactional
    public void cancelByUser(Long userId, Long orderId) {
        orderService.getOrderByIdAndUserId(orderId, userId);

        boolean cancelled = paymentService.tryFailPendingPayment(orderId);
        if (cancelled) {
            orderService.cancelOrder(orderId);
            rollbackStock(orderId);

            eventPublisher.publishEvent(OrderCancelledEvent.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .build());
        }
    }

    private void rollbackStock(Long orderId) {
        orderService.getOrderItems(orderId).forEach(item ->
                stockService.rollbackStock(item.getProductId(), item.getQuantity()));
    }
}
