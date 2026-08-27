package com.dropify.payment.client;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.payment.dto.request.TossCancelRequest;
import com.dropify.payment.dto.request.TossConfirmRequest;
import com.dropify.payment.dto.response.TossPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private final WebClient tossWebClient;

    public void cancel(String paymentKey, String cancelReason) {
        log.debug("토스 결제 취소 요청: paymentKey={}", paymentKey);
        tossWebClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .bodyValue(new TossCancelRequest(cancelReason))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.warn("토스 결제 취소 실패: paymentKey={}, body={}", paymentKey, body);
                            return Mono.error(new BusinessException(ErrorCode.TOSS_CANCEL_ERROR));
                        })
                )
                .bodyToMono(Void.class)
                .block();
    }

    public TossPaymentResponse confirm(String paymentKey, String orderId, Long amount) {
        log.debug("토스 결제 승인 요청: paymentKey={}, orderId={}", paymentKey, orderId);
        return tossWebClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.warn("토스 결제 승인 실패: status={}, body={}", response.statusCode(), body);
                            return Mono.error(new BusinessException(ErrorCode.TOSS_API_ERROR));
                        })
                )
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }
}
