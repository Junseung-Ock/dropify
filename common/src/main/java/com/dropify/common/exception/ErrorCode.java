package com.dropify.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "Invalid input value"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "Internal server error"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_003", "Entity not found"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "User not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "Email already exists"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_003", "Invalid password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "USER_004", "Unauthorized"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "USER_005", "Token expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "USER_006", "Access denied"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "Product not found"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRODUCT_002", "Insufficient stock"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "Order not found"),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "ORDER_002", "Order already cancelled"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "Payment not found"),
    PAYMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_002", "Payment processing failed");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
