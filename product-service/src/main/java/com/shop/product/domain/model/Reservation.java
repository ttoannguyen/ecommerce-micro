package com.shop.product.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A time-bound, idempotent claim on available inventory. */
public final class Reservation {

    private final UUID id;
    private final String caller;
    private final String idempotencyKey;
    private final Long productId;
    private final int quantity;
    private final ReservationStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant updatedAt;
    private final Long version;

    private Reservation(UUID id, String caller, String idempotencyKey,
                        Long productId, int quantity, ReservationStatus status,
                        Instant createdAt, Instant expiresAt, Instant updatedAt,
                        Long version) {
        if (id == null || productId == null || status == null
                || createdAt == null || expiresAt == null || updatedAt == null) {
            throw new IllegalArgumentException("reservation thiếu dữ liệu bắt buộc");
        }
        if (caller == null || caller.isBlank()) {
            throw new IllegalArgumentException("caller là bắt buộc");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key là bắt buộc");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt phải sau createdAt");
        }
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

    public static Reservation hold(UUID id, String caller, String idempotencyKey,
                                   Long productId, int quantity, Instant now,
                                   Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("reservation TTL phải > 0");
        }
        return new Reservation(id, caller.trim(), idempotencyKey.trim(), productId,
                quantity, ReservationStatus.HELD, now, now.plus(ttl), now, null);
    }

    public static Reservation rehydrate(UUID id, String caller, String idempotencyKey,
                                        Long productId, int quantity,
                                        ReservationStatus status, Instant createdAt,
                                        Instant expiresAt, Instant updatedAt,
                                        Long version) {
        return new Reservation(id, caller, idempotencyKey, productId, quantity,
                status, createdAt, expiresAt, updatedAt, version);
    }

    public boolean matches(String requestCaller, Long requestProductId,
                           int requestQuantity) {
        return caller.equals(requestCaller.trim())
                && Objects.equals(productId, requestProductId)
                && quantity == requestQuantity;
    }

    public boolean isDue(Instant now) {
        return status == ReservationStatus.HELD && !expiresAt.isAfter(now);
    }

    public Reservation confirm(Instant now) {
        if (status == ReservationStatus.CONFIRMED) {
            return this;
        }
        requireHeld("confirm");
        if (!expiresAt.isAfter(now)) {
            throw new InvalidReservationTransitionException(
                    "reservation đã hết hạn, không thể confirm");
        }
        return transition(ReservationStatus.CONFIRMED, now);
    }

    public Reservation release(Instant now) {
        if (status == ReservationStatus.RELEASED || status == ReservationStatus.EXPIRED) {
            return this;
        }
        requireHeld("release");
        return transition(ReservationStatus.RELEASED, now);
    }

    public Reservation expire(Instant now) {
        if (status != ReservationStatus.HELD) {
            return this;
        }
        if (!isDue(now)) {
            throw new InvalidReservationTransitionException(
                    "reservation chưa tới expiresAt");
        }
        return transition(ReservationStatus.EXPIRED, now);
    }

    private void requireHeld(String operation) {
        if (status != ReservationStatus.HELD) {
            throw new InvalidReservationTransitionException(
                    "không thể " + operation + " reservation ở trạng thái " + status);
        }
    }

    private Reservation transition(ReservationStatus target, Instant now) {
        return new Reservation(id, caller, idempotencyKey, productId, quantity,
                target, createdAt, expiresAt, now, version);
    }

    public UUID id() { return id; }
    public String caller() { return caller; }
    public String idempotencyKey() { return idempotencyKey; }
    public Long productId() { return productId; }
    public int quantity() { return quantity; }
    public ReservationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant updatedAt() { return updatedAt; }
    public Long version() { return version; }
}
