package com.shop.order.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.adapter.out.messaging.EventEnvelope;
import com.shop.order.adapter.out.messaging.InboxJpaEntity;
import com.shop.order.adapter.out.messaging.InboxRepository;
import com.shop.order.adapter.out.messaging.NotificationJpaEntity;
import com.shop.order.adapter.out.messaging.NotificationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "messaging.outbox", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final InboxRepository inboxRepository;
    private final NotificationRepository notificationRepository;

    public NotificationConsumer(ObjectMapper objectMapper, InboxRepository inboxRepository,
                                NotificationRepository notificationRepository) {
        this.objectMapper = objectMapper;
        this.inboxRepository = inboxRepository;
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "${messaging.topic.orders:order-events}",
            groupId = "${messaging.consumer.group:order-notification}")
    @Transactional
    public void consume(String rawEnvelope) {
        EventEnvelope event = parse(rawEnvelope);
        if (inboxRepository.existsById(event.eventId())) {
            return;
        }

        inboxRepository.save(new InboxJpaEntity(event.eventId(), event.eventType(), Instant.now()));
        notificationRepository.save(new NotificationJpaEntity(event.eventId(),
                Long.valueOf(event.aggregateId()), event.eventType(),
                "Order " + event.aggregateId() + " phát sinh " + event.eventType(),
                Instant.now()));
    }

    private EventEnvelope parse(String rawEnvelope) {
        try {
            return objectMapper.readValue(rawEnvelope, EventEnvelope.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("Event envelope không hợp lệ", failure);
        }
    }
}
