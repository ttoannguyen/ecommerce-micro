package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.StockMovement;

/** Translates between the ledger line and its JPA record. */
final class StockMovementMapper {

    private StockMovementMapper() {
    }

    static StockMovementJpaEntity toEntity(StockMovement movement) {
        return new StockMovementJpaEntity(
                movement.id(),
                movement.productId(),
                movement.quantity(),
                movement.type(),
                movement.reason(),
                movement.occurredAt());
    }

    static StockMovement toDomain(StockMovementJpaEntity entity) {
        return StockMovement.rehydrate(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getType(),
                entity.getReasonCode(),
                entity.getOccurredAt());
    }
}
