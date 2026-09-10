package com.dropify.payment.domain;

import java.time.LocalDateTime;

public record WebhookResult(
        WebhookAction action,
        Long orderId,
        Long amount,
        LocalDateTime paidAt
) {
    public static WebhookResult ignored() {
        return new WebhookResult(WebhookAction.IGNORED, null, null, null);
    }

    public static WebhookResult completed(Long orderId, Long amount, LocalDateTime paidAt) {
        return new WebhookResult(WebhookAction.COMPLETED, orderId, amount, paidAt);
    }

    public static WebhookResult paymentFailed(Long orderId) {
        return new WebhookResult(WebhookAction.PAYMENT_FAILED, orderId, null, null);
    }

    public static WebhookResult externalCancelled(Long orderId) {
        return new WebhookResult(WebhookAction.EXTERNAL_CANCELLED, orderId, null, null);
    }
}
