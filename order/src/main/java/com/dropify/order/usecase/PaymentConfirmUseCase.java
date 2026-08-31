package com.dropify.order.usecase;

import com.dropify.order.dto.request.PaymentConfirmRequest;
import com.dropify.order.dto.response.PaymentConfirmResponse;

public interface PaymentConfirmUseCase {
    PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request);
}
