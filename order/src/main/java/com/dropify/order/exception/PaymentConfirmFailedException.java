package com.dropify.order.exception;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;

public class PaymentConfirmFailedException extends BusinessException {
    public PaymentConfirmFailedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
