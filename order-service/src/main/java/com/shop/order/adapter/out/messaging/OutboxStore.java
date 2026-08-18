package com.shop.order.adapter.out.messaging;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxStore {

    private final OutboxRepository repository;

    public OutboxStore(OutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<OutboxJpaEntity> claimBatch(int size, Instant now) {
        List<OutboxJpaEntity> events = repository.findReady(
                List.of(OutboxStatus.PENDING, OutboxStatus.PROCESSING), now,
                PageRequest.of(0, size));
        events.forEach(event -> event.claim(now));
        repository.flush();
        return events;
    }

    @Transactional
    public void markPublished(UUID eventId, Instant now) {
        repository.findById(eventId).ifPresent(event -> {
            event.markPublished(now);
            repository.flush();
        });
    }

    @Transactional
    public void markRetry(UUID eventId, Instant now, Throwable failure) {
        repository.findById(eventId).ifPresent(event -> {
            event.markRetry(now, failure);
            repository.flush();
        });
    }
}
