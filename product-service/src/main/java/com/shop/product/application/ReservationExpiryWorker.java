package com.shop.product.application;

import com.shop.product.domain.port.in.ReservationUseCase;
import com.shop.product.domain.port.out.ReservationStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "inventory.reservation.expiry-enabled",
        havingValue = "true", matchIfMissing = true)
public class ReservationExpiryWorker {

    private final ReservationStorePort reservationStore;
    private final ReservationUseCase reservationUseCase;
    private final Clock clock;
    private final int batchSize;

    public ReservationExpiryWorker(
            ReservationStorePort reservationStore,
            ReservationUseCase reservationUseCase,
            Clock clock,
            @Value("${inventory.reservation.expiry-batch-size:100}") int batchSize) {
        this.reservationStore = reservationStore;
        this.reservationUseCase = reservationUseCase;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${inventory.reservation.expiry-interval:PT5S}")
    public void expireDueReservations() {
        for (UUID id : reservationStore.findDueIds(clock.instant(), batchSize)) {
            // Each call has its own transaction and locks the reservation row. Multiple
            // instances may select the same ID, but only one can release its balance.
            reservationUseCase.expireIfDue(id);
        }
    }
}
