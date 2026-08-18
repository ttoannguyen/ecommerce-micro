package com.shop.order.adapter.out.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxJpaEntity, UUID> {
}
