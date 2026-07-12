package com.shop.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository Spring Data cho bản ghi JPA (không lộ ra domain). */
interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
