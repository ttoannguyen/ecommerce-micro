package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation", uniqueConstraints =
        @UniqueConstraint(name = "uq_reservation_caller_key_product",
                columnNames = {"caller", "idempotency_key", "product_id"}))
public class ReservationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String caller;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected ReservationJpaEntity() {
    }

    ReservationJpaEntity(UUID id, String caller, String idempotencyKey,
                         Long productId, int quantity, ReservationStatus status,
                         Instant createdAt, Instant expiresAt, Instant updatedAt,
                         Long version) {
        this.id = id;
        this.caller = caller;
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getCaller() { return caller; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
