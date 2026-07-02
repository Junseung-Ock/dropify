package com.dropify.order.event;

import com.dropify.order.dto.response.PlaceOrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String PAYMENT_REQUEST_TOPIC = "payment-request";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // 주문 생성 완료 후 호출
    public void publishPaymentRequest(Long userId, PlaceOrderResponse response) {
        PaymentRequestEvent event = new PaymentRequestEvent(
                response.getOrderId(),
                userId,
                response.getTotalAmount()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            // 메시지 키를 orderId로 설정 → 같은 주문은 항상 같은 파티션으로 전송
            kafkaTemplate.send(PAYMENT_REQUEST_TOPIC, String.valueOf(response.getOrderId()), payload);
            log.info("payment-request 이벤트 발행: orderId={}", response.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("payment-request 이벤트 직렬화 실패: orderId={}", response.getOrderId(), e);
        }
    }
}
