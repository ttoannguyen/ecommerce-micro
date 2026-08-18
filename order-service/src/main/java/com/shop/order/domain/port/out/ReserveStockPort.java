package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;

import java.util.UUID;

/**
 * A command port, not a query port. The old LoadProductPort only asked "how much
 * stock is there?" — and the answer was stale the moment it arrived. This one tells
 * product-service to create a hold, and product-service either does it or refuses.
 * That single decision at the owner is what removes the race.
 */
public interface ReserveStockPort {

    /** Throws InsufficientStockException when product-service refuses. */
    ReservedProduct reserve(Long productId, Quantity quantity, String idempotencyKey);

    /** Compensating action: release exactly this reservation after a failure. */
    void release(UUID reservationId);
}
