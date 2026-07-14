package com.shop.order.adapter.in.web;

import org.springframework.http.HttpStatus;

/**
 * Bảng dịch lỗi -> HTTP. Sống ở adapter/in/web vì HttpStatus là chuyện của
 * transport: domain và application không được biết mã 400/409 là gì.
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
