package com.shop.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for the JPA record. Package-private: the domain never sees it. */
interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
