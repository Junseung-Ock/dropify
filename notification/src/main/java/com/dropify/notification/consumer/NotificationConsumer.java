package com.dropify.notification.consumer;

import com.dropify.event.KafkaTopic;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.dropify.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "notification-group")
    public void onPaymentCompleted(String message) throws JsonProcessingException {
        PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
        notificationService.savePaymentCompletedNotification(event.getUserId(), event.getOrderId());
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_FAILED, groupId = "notification-group")
    public void onPaymentFailed(String message) throws JsonProcessingException {
        PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
        notificationService.savePaymentFailedNotification(event.getUserId(), event.getOrderId());
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_CANCELLED, groupId = "notification-group")
    public void onPaymentCancelled(String message) throws JsonProcessingException {
        PaymentCancelledEvent event = objectMapper.readValue(message, PaymentCancelledEvent.class);
        notificationService.savePaymentCancelledNotification(event.getUserId(), event.getOrderId());
    }

    @KafkaListener(topics = KafkaTopic.ORDER_CANCELLED, groupId = "notification-group")
    public void onOrderCancelled(String message) throws JsonProcessingException {
        OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
        notificationService.saveOrderCancelledNotification(event.getUserId(), event.getOrderId());
    }
}
