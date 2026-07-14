package com.shop.order.domain.model;

import java.time.Instant;

/**
 * Order aggregate root — plain POJO, no framework annotations.
 *
 * Note what is NOT here any more: the stock check. Stock belongs to
 * product-service,
 * so product-service enforces it. An aggregate may only enforce invariants over
 * data
 * it owns; enforcing one over a remote copy is how you oversell.
 */
public class Order {

    private final Long id;
    private final Long productId;
    private final Quantity quantity;
    private final Money totalPrice;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(Long id, Long productId, Quantity quantity,
            Money totalPrice, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Places an order against stock that has already been reserved. */
    public static Order place(ReservedProduct product, Quantity quantity) {
        Money total = product.price().multiply(quantity.value());
        return new Order(null, product.productId(), quantity, total,
                OrderStatus.CREATED, Instant.now());
    }

    /** Rebuilds the aggregate from persistence (id already assigned). */
    public static Order rehydrate(Long id, Long productId, Quantity quantity,
            Money totalPrice, OrderStatus status, Instant createdAt) {
        return new Order(id, productId, quantity, totalPrice, status, createdAt);
    }

    public Long id() {
        return id;
    }

    public Long productId() {
        return productId;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Money totalPrice() {
        return totalPrice;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
