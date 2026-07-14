package com.shop.product.adapter.in.web.dto;

import java.time.Instant;
import java.util.Map;

/** Error body returned to the client. `fields` is only populated for validation errors. */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        Map<String, String> fields) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now(), Map.of());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields) {
        return new ErrorResponse(code, message, Instant.now(), fields);
    }
}
