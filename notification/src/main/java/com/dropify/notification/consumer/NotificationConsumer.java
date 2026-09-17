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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "notification-group")
    public void onPaymentCompleted(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            notificationService.savePaymentCompletedNotification(event.getUserId(), event.getOrderId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_FAILED, groupId = "notification-group")
    public void onPaymentFailed(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            PaymentFailedEvent event = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
            notificationService.savePaymentFailedNotification(event.getUserId(), event.getOrderId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_CANCELLED, groupId = "notification-group")
    public void onPaymentCancelled(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            PaymentCancelledEvent event = objectMapper.readValue(record.value(), PaymentCancelledEvent.class);
            notificationService.savePaymentCancelledNotification(event.getUserId(), event.getOrderId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @KafkaListener(topics = KafkaTopic.ORDER_CANCELLED, groupId = "notification-group")
    public void onOrderCancelled(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            OrderCancelledEvent event = objectMapper.readValue(record.value(), OrderCancelledEvent.class);
            notificationService.saveOrderCancelledNotification(event.getUserId(), event.getOrderId());
        } finally {
            MDC.remove("traceId");
        }
    }

    private void setTraceId(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("traceId");
        if (header != null) {
            MDC.put("traceId", new String(header.value(), StandardCharsets.UTF_8));
        }
    }
}
