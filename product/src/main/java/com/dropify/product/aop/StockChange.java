package com.dropify.product.aop;

import com.dropify.product.domain.entity.StockChangeType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StockChange {
    StockChangeType type();
    String reason() default "";
}
