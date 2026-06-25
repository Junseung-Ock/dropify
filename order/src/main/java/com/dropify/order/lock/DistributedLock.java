package com.dropify.order.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키 — SpEL 표현식. 예: "'order:lock:' + #productId"
     * 메서드 파라미터 이름(#paramName)으로 동적 키 생성 가능.
     */
    String key();

    long waitTime() default 3L;

    long leaseTime() default 5L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
