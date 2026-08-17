package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.MovementType;
import com.shop.product.domain.model.ReasonCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA record for one ledger line. Insert-only: there is deliberately no setter and no
 * update path, because a ledger you can edit is not a ledger.
 *
 * No @Version either — nothing ever contends for an existing row.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    /** STRING, not ORDINAL: reordering the enum must not rewrite history. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ReasonCode reasonCode;

    @Column(nullable = false)
    private Instant occurredAt;

    protected StockMovementJpaEntity() {
    }

    StockMovementJpaEntity(Long id, Long productId, int quantity,
                           MovementType type, ReasonCode reasonCode, Instant occurredAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.type = type;
        this.reasonCode = reasonCode;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public MovementType getType() {
        return type;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
