package com.dropify.common.util;

public class LogMasker {

    private LogMasker() {}

    public static String maskPaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.length() <= 9) {
            return "****";
        }
        return paymentKey.substring(0, 6) + "****" + paymentKey.substring(paymentKey.length() - 3);
    }
}
