package com.shop.order.adapter.out.messaging;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int schemaVersion,
        String payload) {
}
