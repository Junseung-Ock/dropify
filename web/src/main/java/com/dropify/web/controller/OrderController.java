package com.dropify.web.controller;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import com.dropify.common.response.ApiResponse;
import com.dropify.order.dto.request.PlaceOrderRequest;
import com.dropify.order.dto.response.OrderDetailResponse;
import com.dropify.order.dto.response.OrderSummaryResponse;
import com.dropify.order.dto.response.PlaceOrderResponse;
import com.dropify.order.service.OrderService;
import com.dropify.user.security.UserDetailsImpl;
import com.dropify.web.usecase.CancelOrderUseCaseImpl;
import com.dropify.web.usecase.PlaceOrderUseCaseImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "주문")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PlaceOrderUseCaseImpl placeOrderUseCase;
    private final CancelOrderUseCaseImpl cancelOrderUseCase;

    @Operation(summary = "주문 생성", description = "idempotency-key 헤더 필수")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품 없음 (PRODUCT_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재고 부족 (PRODUCT_002) / 동시 주문 초과 (ORDER_003)")
    })
    @PostMapping
    public ApiResponse<PlaceOrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader("idempotency-key") String idempotencyKey,
            @RequestBody @Valid PlaceOrderRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(placeOrderUseCase.execute(userId, request, idempotencyKey));
    }

    @Operation(summary = "내 주문 목록 조회")
    @GetMapping
    public ApiResponse<Page<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(orderService.getMyOrders(userId, pageable));
    }

    @Operation(summary = "주문 상세 조회")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음 (ORDER_001)")
    )
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(orderService.getOrderDetail(userId, orderId));
    }

    @Operation(summary = "주문 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음 (ORDER_001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 취소된 주문 (ORDER_002) / 취소 불가 상태 (ORDER_004)")
    })
    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        Long userId = userDetails.getUser().getId();
        cancelOrderUseCase.cancel(userId, orderId);
        return ApiResponse.ok();
    }
}
