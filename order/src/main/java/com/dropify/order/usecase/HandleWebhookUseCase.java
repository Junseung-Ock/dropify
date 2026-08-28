package com.dropify.order.usecase;

import com.dropify.order.dto.request.TossWebhookEvent;

public interface HandleWebhookUseCase {
    void handle(TossWebhookEvent event);
}
