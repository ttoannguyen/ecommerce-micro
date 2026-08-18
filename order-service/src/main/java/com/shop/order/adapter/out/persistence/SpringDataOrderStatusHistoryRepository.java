package com.shop.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataOrderStatusHistoryRepository
        extends JpaRepository<OrderStatusHistoryJpaEntity, Long> {
    List<OrderStatusHistoryJpaEntity> findByOrderIdOrderBySequence(Long orderId);
    Optional<OrderStatusHistoryJpaEntity> findByOrderIdAndSequence(Long orderId, int sequence);
}
