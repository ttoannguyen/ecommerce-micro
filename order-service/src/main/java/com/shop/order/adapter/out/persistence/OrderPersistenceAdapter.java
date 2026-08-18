package com.shop.order.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.adapter.out.messaging.EventEnvelope;
import com.shop.order.adapter.out.messaging.OrderEventPayload;
import com.shop.order.adapter.out.messaging.OrderEventType;
import com.shop.order.adapter.out.messaging.OutboxJpaEntity;
import com.shop.order.adapter.out.messaging.OutboxRepository;
import com.shop.order.adapter.out.messaging.OutboxStatus;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import com.shop.order.observability.CorrelationIdContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Outbound adapter: implements the load/save ports with Spring Data. */
@Component
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort {

    private final SpringDataOrderRepository repository;
    private final SpringDataOrderItemRepository itemRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository,
                                   SpringDataOrderItemRepository itemRepository,
                                   SpringDataOrderStatusHistoryRepository historyRepository,
                                   OutboxRepository outboxRepository,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        boolean newOrder = order.id() == null;
        OrderJpaEntity previous = newOrder ? null : repository.findById(order.id()).orElse(null);
        String previousStatus = previous == null ? null : previous.getStatus();
        OrderJpaEntity entity = repository.saveAndFlush(OrderMapper.toEntity(order));
        if (newOrder) {
            itemRepository.saveAll(order.items().stream()
                    .map(item -> OrderMapper.toItemEntity(entity.getId(), order.items().indexOf(item), item))
                    .toList());
        }
        order.history().forEach(transition -> historyRepository
                .findByOrderIdAndSequence(entity.getId(), transition.sequence())
                .orElseGet(() -> historyRepository.save(
                        OrderMapper.toHistoryEntity(entity.getId(), transition))));
        OrderEventType eventType = eventType(order, previousStatus);
        if (eventType != null) {
            outboxRepository.save(toOutbox(entity, eventType, order));
        }
        return toDomain(entity);
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream()
                .map(entity -> OrderMapper.toDomain(entity,
                        itemRepository.findByOrderIdOrderByLineNumber(entity.getId()),
                        historyRepository.findByOrderIdOrderBySequence(entity.getId())))
                .toList();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private Order toDomain(OrderJpaEntity entity) {
        return OrderMapper.toDomain(entity,
                itemRepository.findByOrderIdOrderByLineNumber(entity.getId()),
                historyRepository.findByOrderIdOrderBySequence(entity.getId()));
    }

    private OutboxJpaEntity toOutbox(OrderJpaEntity entity, OrderEventType eventType,
                                     Order order) {
        Instant occurredAt = Instant.now();
        String payload = writePayload(OrderEventPayload.from(order));
        String correlationId = CorrelationIdContext.currentOrNull();
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType.name(),
                "Order", entity.getId().toString(), occurredAt, 2, correlationId, payload);
        return new OutboxJpaEntity(envelope.eventId(), envelope.eventType(),
                envelope.aggregateType(), envelope.aggregateId(), envelope.occurredAt(),
                envelope.schemaVersion(), writePayload(envelope), correlationId,
                OutboxStatus.PENDING, 0,
                occurredAt, null, null);
    }

    private String writePayload(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Không thể serialize order event", failure);
        }
    }

    private static OrderEventType eventType(Order order, String previousStatus) {
        if (previousStatus == null) {
            return OrderEventType.ORDER_CREATED;
        }
        if (Objects.equals(previousStatus, order.status().name())) {
            return null;
        }
        return switch (order.status()) {
            case PAID -> OrderEventType.ORDER_PAID;
            case CANCELLED -> OrderEventType.ORDER_CANCELLED;
            default -> null;
        };
    }
}
