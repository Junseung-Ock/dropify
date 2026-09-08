package com.dropify.notification.consumer;

import com.dropify.event.KafkaTopic;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.dropify.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "notification-group")
    public void onPaymentCompleted(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            notificationService.savePaymentCompletedNotification(event.getUserId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("결제 완료 이벤트 역직렬화 실패: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_FAILED, groupId = "notification-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            notificationService.savePaymentFailedNotification(event.getUserId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("결제 실패 이벤트 역직렬화 실패: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopic.ORDER_CANCELLED, groupId = "notification-group")
    public void onOrderCancelled(String message) {
        try {
            OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
            notificationService.saveOrderCancelledNotification(event.getUserId(), event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("주문 취소 이벤트 역직렬화 실패: {}", message, e);
        }
    }
}
