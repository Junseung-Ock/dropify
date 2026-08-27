package com.dropify.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank
    private String paymentKey;

    @NotNull
    private Long orderId;

    @NotNull
    @Positive
    private Long amount;
}
