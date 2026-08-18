package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Reservation;

final class ReservationMapper {
    private ReservationMapper() {
    }

    static ReservationJpaEntity toEntity(Reservation reservation) {
        return new ReservationJpaEntity(
                reservation.id(), reservation.caller(), reservation.idempotencyKey(),
                reservation.productId(), reservation.quantity(), reservation.status(),
                reservation.createdAt(), reservation.expiresAt(), reservation.updatedAt(),
                reservation.version());
    }

    static Reservation toDomain(ReservationJpaEntity entity) {
        return Reservation.rehydrate(
                entity.getId(), entity.getCaller(), entity.getIdempotencyKey(),
                entity.getProductId(), entity.getQuantity(), entity.getStatus(),
                entity.getCreatedAt(), entity.getExpiresAt(), entity.getUpdatedAt(),
                entity.getVersion());
    }
}
