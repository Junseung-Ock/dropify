package com.dropify.payment.event;

import com.dropify.event.KafkaTopic;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        publish(KafkaTopic.PAYMENT_COMPLETED, event.getOrderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        publish(KafkaTopic.PAYMENT_FAILED, event.getOrderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        publish(KafkaTopic.PAYMENT_CANCELLED, event.getOrderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        publish(KafkaTopic.ORDER_CANCELLED, event.getOrderId(), event);
    }

    private void publish(String topic, Long key, Object event) {
        try {
            kafkaTemplate.send(topic, key.toString(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: topic={}", topic, e);
        }
    }
}
