package com.shop.order.adapter.out.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxJpaEntity, java.util.UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxJpaEntity e where e.status in :statuses "
            + "and e.nextAttemptAt <= :now order by e.occurredAt")
    List<OutboxJpaEntity> findReady(Collection<OutboxStatus> statuses, Instant now,
                                    Pageable pageable);
}
