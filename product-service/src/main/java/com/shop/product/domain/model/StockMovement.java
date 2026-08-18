package com.shop.product.domain.model;

import java.time.Instant;

/**
 * One line of the stock ledger — an immutable fact about something that happened.
 *
 * The ledger is append-only. Nothing here is ever updated or deleted, because the
 * question it exists to answer is "why is the stock 7?", and an overwritten row
 * cannot answer that. The current stock on Product is a projection of these lines,
 * not an independent number: balance == SUM(quantity).
 *
 * That is the same instinct as refusing to record a failed run as a score of zero.
 * A count that dropped by 3 because of damage and a count that dropped by 3 because
 * of a sale are different events, and collapsing both into "stock -= 3" throws away
 * the only information anyone will later ask for.
 */
public final class StockMovement {

    private final Long id;
    private final Long productId;
    /** Signed: positive adds to the balance, negative removes from it. */
    private final int quantity;
    private final MovementType type;
    private final ReasonCode reason;
    private final Instant occurredAt;

    private StockMovement(Long id, Long productId, int quantity,
                          MovementType type, ReasonCode reason, Instant occurredAt) {
        if (productId == null) {
            throw new IllegalArgumentException("movement phải thuộc về một product");
        }
        if (!type.allows(quantity)) {
            throw new IllegalArgumentException(
                    "quantity " + quantity + " không hợp lệ cho movement " + type);
        }
        if (type.requiresReason() && reason == null) {
            throw new IllegalArgumentException("ADJUSTMENT bắt buộc có lý do");
        }
        if (!type.requiresReason() && reason != null) {
            throw new IllegalArgumentException(type + " không được mang lý do điều chỉnh");
        }
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.type = type;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    static StockMovement receipt(Long productId, int quantity) {
        return new StockMovement(null, productId, quantity, MovementType.RECEIPT, null, Instant.now());
    }

    static StockMovement issue(Long productId, int quantity) {
        return new StockMovement(null, productId, -quantity, MovementType.ISSUE, null, Instant.now());
    }

    static StockMovement adjustment(Long productId, int delta, ReasonCode reason) {
        return new StockMovement(null, productId, delta, MovementType.ADJUSTMENT, reason, Instant.now());
    }

    /** Rebuilds a line that is already in the ledger. */
    public static StockMovement rehydrate(Long id, Long productId, int quantity,
                                          MovementType type, ReasonCode reason, Instant occurredAt) {
        return new StockMovement(id, productId, quantity, type, reason, occurredAt);
    }

    public Long id() {
        return id;
    }

    public Long productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    public MovementType type() {
        return type;
    }

    public ReasonCode reason() {
        return reason;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
