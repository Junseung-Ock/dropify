package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.notification.dto.response.NotificationResponse;
import com.dropify.notification.service.NotificationService;
import com.dropify.payment.dto.response.PaymentHistoryResponse;
import com.dropify.payment.service.PaymentHistoryService;
import com.dropify.user.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Mypage", description = "마이페이지")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final PaymentHistoryService paymentHistoryService;
    private final NotificationService notificationService;

    @Operation(summary = "결제 내역 조회")
    @GetMapping("/payment-histories")
    public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistories(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(paymentHistoryService.getHistories(userId));
    }

    @Operation(summary = "알림 목록 조회")
    @GetMapping("/notifications")
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(notificationService.getNotifications(userId));
    }
}
