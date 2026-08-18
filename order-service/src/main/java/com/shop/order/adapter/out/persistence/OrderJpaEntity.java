package com.shop.order.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** JPA record for Order. Separate from the domain aggregate — annotations live here. */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Explicit, because V1__init.sql states the same thing. Left implicit, Hibernate
    // would assume nullable varchar(255) and numeric(38,2) — and `validate` does not
    // compare length, precision or nullability, so the drift would go unnoticed.
    @Column(nullable = false)
    private Long productId;

    @Column
    private UUID reservationId;

    @Column(length = 128, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 32)
    private String status;

    /** Instant, so this is timestamptz — a wall-clock `timestamp` would lose the zone. */
    @Column(nullable = false)
    private Instant createdAt;

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(Long id, Long productId, UUID reservationId,
                          String idempotencyKey, int quantity,
                          BigDecimal totalPrice, String status, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.reservationId = reservationId;
        this.idempotencyKey = idempotencyKey;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
