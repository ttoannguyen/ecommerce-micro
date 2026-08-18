package com.shop.order.adapter.out.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxJpaEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private int schemaVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(length = 128)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private Instant publishedAt;

    protected OutboxJpaEntity() {
    }

    public OutboxJpaEntity(UUID eventId, String eventType, String aggregateType,
                           String aggregateId, Instant occurredAt, int schemaVersion,
                           String payload, String correlationId, OutboxStatus status, int attempts,
                           Instant nextAttemptAt, String lastError, Instant publishedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.schemaVersion = schemaVersion;
        this.payload = payload;
        this.correlationId = correlationId;
        this.status = status;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.publishedAt = publishedAt;
    }

    public void claim(Instant now) {
        status = OutboxStatus.PROCESSING;
        attempts++;
        nextAttemptAt = now.plusSeconds(30);
    }

    public void markPublished(Instant now) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        lastError = null;
    }

    public void markRetry(Instant now, Throwable failure) {
        status = OutboxStatus.PENDING;
        long delaySeconds = Math.min(60, 1L << Math.min(attempts, 6));
        nextAttemptAt = now.plusSeconds(delaySeconds);
        lastError = failure.toString().substring(0, Math.min(2000, failure.toString().length()));
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public Instant getOccurredAt() { return occurredAt; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
}
