package com.shop.product.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String requested = request.getHeader(CorrelationIdContext.HEADER);
        String correlationId = requested == null || requested.isBlank()
                ? CorrelationIdContext.ensure() : requested.trim();
        if (correlationId.length() > 128) {
            correlationId = correlationId.substring(0, 128);
        }
        CorrelationIdContext.set(correlationId);
        response.setHeader(CorrelationIdContext.HEADER, correlationId);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.atInfo()
                    .addKeyValue("correlationId", correlationId)
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", (System.nanoTime() - started) / 1_000_000)
                    .log("request.completed");
            CorrelationIdContext.clear();
        }
    }
}
