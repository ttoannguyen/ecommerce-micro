package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;

/**
 * A command port, not a query port. The old LoadProductPort only asked "how much
 * stock is there?" — and the answer was stale the moment it arrived. This one tells
 * product-service to actually take the stock, and product-service either does it or
 * refuses. That single change is what removes the race.
 */
public interface ReserveStockPort {

    /** Throws InsufficientStockException when product-service refuses. */
    ReservedProduct reserve(Long productId, Quantity quantity);

    /** Compensating action: hand the stock back after a failure. */
    void release(Long productId, Quantity quantity);
}
