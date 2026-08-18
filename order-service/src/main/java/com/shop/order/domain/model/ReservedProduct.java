package com.shop.order.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Proof that product-service created a time-bound hold inside its own transaction.
 * Anti-corruption boundary of the Order context.
 *
 * Deliberately carries no `stock` field: Order must not be tempted to re-check an
 * invariant it does not own. The old ProductSnapshot did carry stock, and that was
 * exactly the bug — checking a copy that was already stale by the time we saved.
 */
public final class ReservedProduct {

    private final Long productId;
    private final UUID reservationId;
    private final String name;
    private final Money price;
    private final int quantity;
    private final Instant expiresAt;

    public ReservedProduct(Long productId, UUID reservationId, String name,
                           Money price, Instant expiresAt) {
        this(productId, reservationId, name, price, 1, expiresAt);
    }

    public ReservedProduct(Long productId, UUID reservationId, String name,
                           Money price, int quantity, Instant expiresAt) {
        this.productId = productId;
        this.reservationId = reservationId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public Long productId() {
        return productId;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }

    public int quantity() {
        return quantity;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
