package com.dropify.event;

public final class KafkaTopic {

    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String STOCK_CHANGED = "stock.changed";

    private KafkaTopic() {}
}
