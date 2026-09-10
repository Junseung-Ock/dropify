package com.dropify.payment.domain;

public enum WebhookAction {
    COMPLETED,
    PAYMENT_FAILED,
    EXTERNAL_CANCELLED,
    IGNORED
}
