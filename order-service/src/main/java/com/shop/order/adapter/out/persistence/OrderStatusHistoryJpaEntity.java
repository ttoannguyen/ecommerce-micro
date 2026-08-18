package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistoryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private int sequence;
    @Enumerated(EnumType.STRING)
    private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    private OrderStatus toStatus;
    private Instant transitionedAt;

    protected OrderStatusHistoryJpaEntity() {
    }

    public OrderStatusHistoryJpaEntity(Long id, Long orderId, int sequence,
                                       OrderStatus fromStatus, OrderStatus toStatus,
                                       Instant transitionedAt) {
        this.id = id;
        this.orderId = orderId;
        this.sequence = sequence;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.transitionedAt = transitionedAt;
    }

    public int getSequence() { return sequence; }
    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public Instant getTransitionedAt() { return transitionedAt; }
}
