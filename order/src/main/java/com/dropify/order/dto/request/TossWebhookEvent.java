package com.dropify.order.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossWebhookEvent {
    private String paymentKey;
    private String orderId;
    private String status;
    private String secret;
    private String type;
}
