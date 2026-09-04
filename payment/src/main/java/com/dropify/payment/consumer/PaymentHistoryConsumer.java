package com.dropify.payment.consumer;

import com.dropify.event.KafkaTopic;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.payment.service.PaymentHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHistoryConsumer {

    private final PaymentHistoryService paymentHistoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopic.PAYMENT_COMPLETED, groupId = "payment-history-group")
    public void onPaymentCompleted(String message) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            paymentHistoryService.save(event.getUserId(), event.getOrderId(), event.getAmount(), event.getPaidAt());
        } catch (JsonProcessingException e) {
            log.error("결제 내역 이벤트 역직렬화 실패: message={}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopic.PAYMENT_CANCELLED, groupId = "payment-history-group")
    public void onPaymentCancelled(String message) {
        try {
            PaymentCancelledEvent event = objectMapper.readValue(message, PaymentCancelledEvent.class);
            paymentHistoryService.cancel(event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("결제 취소 이벤트 역직렬화 실패: message={}", message, e);
        }
    }
}
