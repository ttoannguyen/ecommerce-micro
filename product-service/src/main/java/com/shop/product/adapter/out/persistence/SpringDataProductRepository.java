package com.shop.product.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Spring Data repository for the JPA record. */
interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, Long> {

    /**
     * Emits SELECT ... FOR UPDATE. The first transaction to reach this row holds it
     * until commit; everyone else blocks here. That is what turns twenty simultaneous
     * "is there enough stock?" questions into twenty sequential ones.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductJpaEntity p where p.id = :id")
    Optional<ProductJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
