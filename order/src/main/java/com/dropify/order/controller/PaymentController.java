package com.dropify.order.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.order.dto.request.PaymentConfirmRequest;
import com.dropify.order.dto.request.TossWebhookEvent;
import com.dropify.order.dto.response.PaymentConfirmResponse;
import com.dropify.order.service.PaymentService;
import com.dropify.user.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PaymentConfirmRequest request) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(paymentService.confirm(userId, request));
    }

    @GetMapping("/fail")
    public ApiResponse<Void> fail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String orderId) {
        Long userId = userDetails.getUser().getId();
        Long id = Long.parseLong(orderId.replace("order-", ""));
        paymentService.cancelByUser(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody TossWebhookEvent event) {
        paymentService.handleWebhook(event);
        return ApiResponse.ok();
    }
}
