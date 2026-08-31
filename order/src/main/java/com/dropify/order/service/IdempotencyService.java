package com.dropify.order.service;

import com.dropify.order.dto.response.PlaceOrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration RESERVE_TTL = Duration.ofSeconds(30);
    private static final String PROCESSING = "__PROCESSING__";

    private final StringRedisTemplate redisTemplate;
    // Redis는 문자열만 저장할 수 있어서, PlaceOrderResponse 객체를 바로 넣을 수 없음
    // StringRedisTemplate + ObjectMapper 조합으로 객체를 JSON 문자열로 변환해서 저장하고, 꺼낼 때 다시 객체로 복원
    private final ObjectMapper objectMapper;

    public Optional<PlaceOrderResponse> get(Long userId, String idempotencyKey) {
        String cached = redisTemplate.opsForValue().get(buildKey(userId, idempotencyKey));
        if (cached == null || PROCESSING.equals(cached)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, PlaceOrderResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("멱등성 캐시 역직렬화 실패: key={}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    public boolean reserve(Long userId, String idempotencyKey) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(buildKey(userId, idempotencyKey), PROCESSING, RESERVE_TTL));
    }

    public void complete(Long userId, String idempotencyKey, PlaceOrderResponse response) {
        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(buildKey(userId, idempotencyKey), value, TTL);
        } catch (JsonProcessingException e) {
            log.warn("멱등성 캐시 저장 실패: key={}", idempotencyKey, e);
        }
    }

    public void release(Long userId, String idempotencyKey) {
        redisTemplate.delete(buildKey(userId, idempotencyKey));
    }

    // userId로 스코프를 나눠 다른 사용자의 키 재사용을 방지
    private String buildKey(Long userId, String idempotencyKey) {
        return PREFIX + userId + ":" + idempotencyKey;
    }
}
