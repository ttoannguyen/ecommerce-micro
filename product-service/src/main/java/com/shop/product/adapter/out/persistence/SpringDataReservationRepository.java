package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataReservationRepository extends JpaRepository<ReservationJpaEntity, UUID> {

    Optional<ReservationJpaEntity> findByCallerAndIdempotencyKeyAndProductId(
            String caller, String idempotencyKey, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationJpaEntity r where r.id = :id")
    Optional<ReservationJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select r.id from ReservationJpaEntity r "
            + "where r.status = :status and r.expiresAt <= :now order by r.expiresAt")
    List<UUID> findDueIds(@Param("status") ReservationStatus status,
                          @Param("now") Instant now, Pageable pageable);

    @Query("select coalesce(sum(r.quantity), 0) from ReservationJpaEntity r "
            + "where r.productId = :productId and r.status = :status")
    int quantityByProductAndStatus(@Param("productId") Long productId,
                                   @Param("status") ReservationStatus status);
}
