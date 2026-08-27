package com.dropify.order.controller;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.common.response.ApiResponse;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.OrderDetailResponse;
import com.dropify.order.dto.response.OrderSummaryResponse;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.OrderService;
import com.dropify.order.service.PaymentService;
import com.dropify.user.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PlaceOrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("idempotency-key") String idempotencyKey,
            @RequestBody @Valid PlaceOrderRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(orderService.placeOrder(userId, request, idempotencyKey));
    }

    @GetMapping
    public ApiResponse<Page<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(orderService.getMyOrders(userId, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(orderService.getOrderDetail(userId, orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        Long userId = userDetails.getUser().getId();
        paymentService.cancelOrder(userId, orderId);
        return ApiResponse.ok();
    }
}
