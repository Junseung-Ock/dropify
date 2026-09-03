package com.dropify.product.event;

import com.dropify.event.KafkaTopic;
import com.dropify.event.StockChangedEvent;
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
public class StockEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockChangedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopic.STOCK_CHANGED, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("재고 변경 이벤트 직렬화 실패: productId={}", event.getProductId(), e);
        }
    }
}
