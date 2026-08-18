package com.shop.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository for the JPA record. Package-private: the domain never sees it. */
interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
