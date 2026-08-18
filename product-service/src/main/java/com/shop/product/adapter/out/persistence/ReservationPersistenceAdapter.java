package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Reservation;
import com.shop.product.domain.model.ReservationStatus;
import com.shop.product.domain.port.out.ReservationStorePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservationPersistenceAdapter implements ReservationStorePort {

    private final SpringDataReservationRepository repository;

    public ReservationPersistenceAdapter(SpringDataReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Reservation> findByCallerAndKey(String caller, String key) {
        return repository.findByCallerAndIdempotencyKey(caller, key)
                .map(ReservationMapper::toDomain);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return repository.findById(id).map(ReservationMapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(ReservationMapper::toDomain);
    }

    @Override
    public Reservation save(Reservation reservation) {
        return ReservationMapper.toDomain(
                repository.saveAndFlush(ReservationMapper.toEntity(reservation)));
    }

    @Override
    public List<UUID> findDueIds(Instant now, int limit) {
        return repository.findDueIds(ReservationStatus.HELD, now,
                PageRequest.of(0, limit));
    }

    @Override
    public int heldQuantityFor(Long productId) {
        return repository.quantityByProductAndStatus(productId, ReservationStatus.HELD);
    }
}
