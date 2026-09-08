package com.dropify.event;

public final class KafkaTopic {

    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";
    public static final String ORDER_CANCELLED = "order.cancelled";

    public static final String PAYMENT_COMPLETED_DLQ = "payment.completed.dlq";
    public static final String PAYMENT_FAILED_DLQ = "payment.failed.dlq";
    public static final String PAYMENT_CANCELLED_DLQ = "payment.cancelled.dlq";
    public static final String ORDER_CANCELLED_DLQ = "order.cancelled.dlq";

    private KafkaTopic() {}
}
