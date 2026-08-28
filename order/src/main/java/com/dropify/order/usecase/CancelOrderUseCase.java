package com.dropify.order.usecase;

public interface CancelOrderUseCase {
    void cancel(Long userId, Long orderId);
    void cancelByUser(Long userId, Long orderId);
}
