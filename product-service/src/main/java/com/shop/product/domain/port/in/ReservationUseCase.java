package com.shop.product.domain.port.in;

import com.shop.product.domain.model.ReservationBatchDetails;
import com.shop.product.domain.model.ReservationDetails;

import java.util.Optional;
import java.util.UUID;

public interface ReservationUseCase {
    ReservationDetails hold(HoldReservationCommand command);
    ReservationBatchDetails holdBatch(HoldReservationBatchCommand command);
    Optional<ReservationDetails> findById(UUID id);
    ReservationDetails confirm(UUID id);
    ReservationDetails release(UUID id);
    void expireIfDue(UUID id);
}
