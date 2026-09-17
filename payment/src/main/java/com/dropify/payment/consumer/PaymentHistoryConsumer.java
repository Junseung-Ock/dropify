package com.dropify.payment.consumer;

import com.dropify.event.KafkaTopic;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.payment.service.PaymentHistoryService;
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
public class PaymentHistoryConsumer {

    private final PaymentHistoryService paymentHistoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "payment-history-group")
    public void onPaymentCompleted(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            paymentHistoryService.save(event.getUserId(), event.getOrderId(), event.getAmount(), event.getPaidAt());
        } finally {
            MDC.remove("traceId");
        }
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_CANCELLED, groupId = "payment-history-group")
    public void onPaymentCancelled(ConsumerRecord<String, String> record) throws JsonProcessingException {
        setTraceId(record);
        try {
            log.info("[CONSUME] topic={}, key={}", record.topic(), record.key());
            PaymentCancelledEvent event = objectMapper.readValue(record.value(), PaymentCancelledEvent.class);
            paymentHistoryService.cancel(event.getOrderId());
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
