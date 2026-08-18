package com.shop.order.domain.model;

import java.util.UUID;

/** Immutable order-line snapshot returned by inventory at reservation time. */
public record OrderItem(Long productId, UUID reservationId, String name,
                        Money unitPrice, Quantity quantity) {

    public OrderItem {
        if (productId == null || reservationId == null || name == null || name.isBlank()
                || unitPrice == null || quantity == null) {
            throw new IllegalArgumentException("order item thiếu dữ liệu bắt buộc");
        }
    }

    public Money totalPrice() {
        return unitPrice.multiply(quantity.value());
    }
}
