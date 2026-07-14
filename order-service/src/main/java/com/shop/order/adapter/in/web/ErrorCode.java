package com.shop.order.adapter.in.web;

import org.springframework.http.HttpStatus;

/**
 * Maps failures to HTTP. Lives in adapter/in/web because HttpStatus is a transport
 * concern: neither the domain nor the application layer may know what a 400 is.
 */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    PRODUCT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
