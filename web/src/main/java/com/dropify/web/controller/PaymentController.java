package com.dropify.web.controller;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.common.response.ApiResponse;
import com.dropify.payment.dto.request.PaymentConfirmRequest;
import com.dropify.payment.dto.request.TossWebhookEvent;
import com.dropify.payment.dto.response.PaymentConfirmResponse;
import com.dropify.user.security.UserDetailsImpl;
import com.dropify.web.usecase.CancelOrderUseCaseImpl;
import com.dropify.web.usecase.HandleWebhookUseCaseImpl;
import com.dropify.web.usecase.PaymentConfirmUseCaseImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment", description = "결제")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentConfirmUseCaseImpl paymentConfirmUseCase;
    private final CancelOrderUseCaseImpl cancelOrderUseCase;
    private final HandleWebhookUseCaseImpl handleWebhookUseCase;

    @Operation(summary = "결제 승인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 입력값 (COMMON_001) / 결제 금액 불일치 (PAYMENT_004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 결제 (PAYMENT_005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Toss API 호출 실패 (PAYMENT_003)")
    })
    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PaymentConfirmRequest request) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(paymentConfirmUseCase.confirm(userId, request));
    }

    @Operation(summary = "결제 실패 처리")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 주문 ID 형식 (COMMON_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음 (ORDER_001)")
    })
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

    @Operation(summary = "토스 웹훅 수신", security = {})
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음 (ORDER_001)")
    )
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody TossWebhookEvent event) {
        handleWebhookUseCase.handle(event);
        return ApiResponse.ok();
    }
}
