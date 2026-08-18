package com.shop.order.observability;

import org.slf4j.MDC;

import java.util.UUID;

/** Small request context used by REST, Feign and the transactional outbox. */
public final class CorrelationIdContext {

    public static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    private CorrelationIdContext() {
    }

    public static String currentOrNull() {
        return MDC.get(MDC_KEY);
    }

    public static String ensure() {
        String current = currentOrNull();
        if (current == null || current.isBlank()) {
            current = UUID.randomUUID().toString();
            MDC.put(MDC_KEY, current);
        }
        return current;
    }

    public static void set(String value) {
        MDC.put(MDC_KEY, value);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
