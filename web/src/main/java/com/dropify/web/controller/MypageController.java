package com.dropify.web.controller;

import com.dropify.common.response.ApiResponse;
import com.dropify.payment.dto.response.PaymentHistoryResponse;
import com.dropify.payment.service.PaymentHistoryService;
import com.dropify.user.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping("/payment-histories")
    public ApiResponse<List<PaymentHistoryResponse>> getPaymentHistories(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.ok(paymentHistoryService.getHistories(userId));
    }
}
