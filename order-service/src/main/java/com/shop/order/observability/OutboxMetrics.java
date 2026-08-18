package com.shop.order.observability;

import com.shop.order.adapter.out.messaging.OutboxRepository;
import com.shop.order.adapter.out.messaging.OutboxStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Exposes the operational signal that cannot be inferred from HTTP metrics: backlog. */
@Component
public class OutboxMetrics {

    public OutboxMetrics(OutboxRepository repository, MeterRegistry registry) {
        register(registry, repository, "pending", OutboxStatus.PENDING);
        register(registry, repository, "processing", OutboxStatus.PROCESSING);
        register(registry, repository, "published", OutboxStatus.PUBLISHED);
    }

    private void register(MeterRegistry registry, OutboxRepository repository,
                          String status, OutboxStatus outboxStatus) {
        Gauge.builder("messaging.outbox.events", repository,
                        source -> source.countByStatus(outboxStatus))
                .description("Number of outbox events by status")
                .tag("status", status)
                .register(registry);
    }
}
