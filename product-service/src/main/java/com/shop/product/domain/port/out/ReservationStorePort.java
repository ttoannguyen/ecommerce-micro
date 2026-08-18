package com.shop.product.domain.port.out;

import com.shop.product.domain.model.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationStorePort {
    Optional<Reservation> findByCallerKeyAndProduct(String caller, String idempotencyKey,
                                                     Long productId);
    Optional<Reservation> findById(UUID id);
    Optional<Reservation> findByIdForUpdate(UUID id);
    Reservation save(Reservation reservation);
    List<UUID> findDueIds(Instant now, int limit);
    int heldQuantityFor(Long productId);
}
