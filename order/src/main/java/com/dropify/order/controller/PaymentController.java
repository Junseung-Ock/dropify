package com.dropify.order.controller;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.common.response.ApiResponse;
import com.dropify.order.dto.request.PaymentConfirmRequest;
import com.dropify.order.dto.request.TossWebhookEvent;
import com.dropify.order.dto.response.PaymentConfirmResponse;
import com.dropify.order.usecase.CancelOrderUseCase;
import com.dropify.order.usecase.HandleWebhookUseCase;
import com.dropify.order.usecase.PaymentConfirmUseCase;
import com.dropify.user.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentConfirmUseCase paymentConfirmUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final HandleWebhookUseCase handleWebhookUseCase;

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PaymentConfirmRequest request) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(paymentConfirmUseCase.confirm(userId, request));
    }

    @GetMapping("/fail")
    public ApiResponse<Void> fail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String orderId) {
        Long userId = userDetails.getUser().getId();
        Long id;
        try {
            id = Long.parseLong(orderId.replace("order-", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        cancelOrderUseCase.cancelByUser(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody TossWebhookEvent event) {
        handleWebhookUseCase.handle(event);
        return ApiResponse.ok();
    }
}
