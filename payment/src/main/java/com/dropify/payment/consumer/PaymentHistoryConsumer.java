package com.dropify.payment.consumer;

import com.dropify.event.KafkaTopic;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.payment.service.PaymentHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentHistoryConsumer {

    private final PaymentHistoryService paymentHistoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "payment-history-group")
    public void onPaymentCompleted(String message) throws JsonProcessingException {
        PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
        paymentHistoryService.save(event.getUserId(), event.getOrderId(), event.getAmount(), event.getPaidAt());
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_CANCELLED, groupId = "payment-history-group")
    public void onPaymentCancelled(String message) throws JsonProcessingException {
        PaymentCancelledEvent event = objectMapper.readValue(message, PaymentCancelledEvent.class);
        paymentHistoryService.cancel(event.getOrderId());
    }
}
