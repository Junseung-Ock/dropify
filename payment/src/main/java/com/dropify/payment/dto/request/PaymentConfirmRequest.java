package com.dropify.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @Schema(example = "tgen_20240101000000AbCd1")
    @NotBlank
    private String paymentKey;

    @Schema(example = "1")
    @NotNull
    private Long orderId;

    @Schema(example = "29800")
    @NotNull
    @Positive
    private Long amount;
}
