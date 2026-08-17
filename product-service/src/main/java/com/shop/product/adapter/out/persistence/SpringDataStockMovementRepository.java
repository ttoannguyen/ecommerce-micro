package com.shop.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for the ledger. */
interface SpringDataStockMovementRepository extends JpaRepository<StockMovementJpaEntity, Long> {

    /**
     * COALESCE, because a product with no movements has a balance of 0, not null.
     * "No rows" and "sums to zero" are the same answer here; elsewhere they would not be.
     */
    @Query("select coalesce(sum(m.quantity), 0) from StockMovementJpaEntity m where m.productId = :productId")
    int balanceOf(@Param("productId") Long productId);
}
