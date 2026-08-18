package com.shop.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataOrderItemRepository extends JpaRepository<OrderItemJpaEntity, Long> {
    List<OrderItemJpaEntity> findByOrderIdOrderByLineNumber(Long orderId);
}
