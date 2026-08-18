package com.shop.product.observability;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIdContext {

    public static final String HEADER = "X-Correlation-Id";

    private CorrelationIdContext() {
    }

    public static String ensure() {
        String current = MDC.get("correlationId");
        if (current == null || current.isBlank()) {
            current = UUID.randomUUID().toString();
            MDC.put("correlationId", current);
        }
        return current;
    }

    public static void set(String value) {
        MDC.put("correlationId", value);
    }

    public static void clear() {
        MDC.remove("correlationId");
    }
}
