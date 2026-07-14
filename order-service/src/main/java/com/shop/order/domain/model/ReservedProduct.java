package com.shop.order.domain.model;

/**
 * Proof that product-service has already taken the stock off the shelf for us,
 * inside its own transaction. Anti-corruption boundary of the Order context.
 *
 * Deliberately carries no `stock` field: Order must not be tempted to re-check an
 * invariant it does not own. The old ProductSnapshot did carry stock, and that was
 * exactly the bug — checking a copy that was already stale by the time we saved.
 */
public final class ReservedProduct {

    private final Long productId;
    private final String name;
    private final Money price;

    public ReservedProduct(Long productId, String name, Money price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public Long productId() {
        return productId;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }
}
