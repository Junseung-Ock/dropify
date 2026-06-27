package com.dropify.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestEvent {

    private Long orderId;
    private Long userId;
    private Long totalAmount;
}
