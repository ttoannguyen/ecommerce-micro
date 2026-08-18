package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.model.OrderItemDraft;

import java.util.List;
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

    /** Atomically reserves every requested SKU or throws without a partial hold. */
    default List<ReservedProduct> reserve(List<OrderItemDraft> items, String idempotencyKey) {
        return items.stream()
                .map(item -> reserve(item.productId(), item.quantity(),
                        idempotencyKey + ":" + item.productId()))
                .toList();
    }

    /** Compensating action: release exactly this reservation after a failure. */
    void release(UUID reservationId);
}
