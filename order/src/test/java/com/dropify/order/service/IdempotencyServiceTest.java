package com.dropify.order.service;

import com.dropify.order.domain.entity.OrderStatus;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Redis에 캐시가 없으면 Optional.empty()를 반환한다")
    void get_keyNotExists_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<PlaceOrderResponse> result = idempotencyService.get(1L, "test-key");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Redis에 캐시가 있으면 역직렬화된 응답을 반환한다")
    void get_keyExists_returnsDeserializedResponse() throws JsonProcessingException {
        PlaceOrderResponse expected = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("{\"orderId\":1}");
        when(objectMapper.readValue(anyString(), eq(PlaceOrderResponse.class))).thenReturn(expected);

        Optional<PlaceOrderResponse> result = idempotencyService.get(1L, "test-key");

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("역직렬화 실패 시 Optional.empty()를 반환한다")
    void get_deserializationFails_returnsEmpty() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("invalid-json");
        when(objectMapper.readValue(anyString(), eq(PlaceOrderResponse.class)))
                .thenThrow(mock(JsonProcessingException.class));

        Optional<PlaceOrderResponse> result = idempotencyService.get(1L, "test-key");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save() 호출 시 직렬화 후 Redis에 저장한다")
    void save_serializesAndStoresInRedis() throws JsonProcessingException {
        PlaceOrderResponse response = new PlaceOrderResponse(1L, OrderStatus.PENDING, 10000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(response)).thenReturn("{\"orderId\":1}");

        idempotencyService.save(1L, "test-key", response);

        verify(valueOperations).set(eq("idempotency:1:test-key"), eq("{\"orderId\":1}"), any());
    }
}
