package com.shop.order.domain.model;

import java.time.Instant;
import java.util.UUID;

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
    private final UUID reservationId;
    private final String idempotencyKey;
    private final Quantity quantity;
    private final Money totalPrice;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(Long id, Long productId, UUID reservationId, String idempotencyKey,
            Quantity quantity,
            Money totalPrice, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.reservationId = reservationId;
        this.idempotencyKey = idempotencyKey;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Places an order against stock that has already been reserved. */
    public static Order place(ReservedProduct product, Quantity quantity,
                              String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key là bắt buộc");
        }
        Money total = product.price().multiply(quantity.value());
        return new Order(null, product.productId(), product.reservationId(),
                idempotencyKey.trim(), quantity, total,
                OrderStatus.CREATED, Instant.now());
    }

    /** Rebuilds the aggregate from persistence (id already assigned). */
    public static Order rehydrate(Long id, Long productId, UUID reservationId,
            String idempotencyKey, Quantity quantity,
            Money totalPrice, OrderStatus status, Instant createdAt) {
        return new Order(id, productId, reservationId, idempotencyKey,
                quantity, totalPrice, status, createdAt);
    }

    public Long id() {
        return id;
    }

    public Long productId() {
        return productId;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public boolean matchesRequest(Long requestedProductId, int requestedQuantity) {
        return productId.equals(requestedProductId)
                && quantity.value() == requestedQuantity;
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
