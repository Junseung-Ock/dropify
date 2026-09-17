package com.dropify.payment.event;

import com.dropify.event.KafkaTopic;
import com.dropify.event.OrderCancelledEvent;
import com.dropify.event.PaymentCancelledEvent;
import com.dropify.event.PaymentCompletedEvent;
import com.dropify.event.PaymentFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

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
            String json = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key.toString(), json);
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));
            }
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        Map<String, String> previous = MDC.getCopyOfContextMap();
                        if (mdcContext != null) MDC.setContextMap(mdcContext);
                        try {
                            if (ex != null) {
                                log.error("[PRODUCE] Kafka 전송 실패: topic={}, key={}", topic, key, ex);
                                meterRegistry.counter("dropify.kafka.publish.failure", "topic", topic).increment();
                            } else {
                                log.info("[PRODUCE] topic={}, key={}", topic, key);
                            }
                        } finally {
                            if (previous != null) MDC.setContextMap(previous);
                            else MDC.clear();
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: topic={}", topic, e);
            meterRegistry.counter("dropify.kafka.publish.failure", "topic", topic).increment();
        }
    }
}
