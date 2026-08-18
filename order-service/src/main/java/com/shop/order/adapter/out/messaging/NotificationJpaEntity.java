package com.shop.order.adapter.out.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(UUID eventId, Long orderId, String eventType,
                                 String message, Instant createdAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.message = message;
        this.createdAt = createdAt;
    }
}
