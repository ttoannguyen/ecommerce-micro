package com.shop.order.adapter.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "messaging.outbox", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxStore store;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisher(OutboxStore store, KafkaTemplate<String, String> kafkaTemplate,
                           org.springframework.core.env.Environment environment) {
        this.store = store;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = environment.getProperty("messaging.topic.orders", "order-events");
        this.batchSize = environment.getProperty("messaging.outbox.batch-size", Integer.class, 50);
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxJpaEntity> events = store.claimBatch(batchSize, Instant.now());
        events.forEach(this::publish);
    }

    private void publish(OutboxJpaEntity event) {
        try {
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
            store.markPublished(event.getEventId(), Instant.now());
        } catch (Exception failure) {
            store.markRetry(event.getEventId(), Instant.now(), failure);
            log.warn("Không publish được outbox event {}, sẽ retry", event.getEventId(), failure);
        }
    }
}
