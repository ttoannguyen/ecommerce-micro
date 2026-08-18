package com.shop.order.observability;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/** Propagates the REST correlation context to product-service. */
@Component
public class CorrelationIdFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = CorrelationIdContext.currentOrNull();
        if (correlationId != null && !correlationId.isBlank()) {
            template.header(CorrelationIdContext.HEADER, correlationId);
        }
    }
}
