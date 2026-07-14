package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.Product;

import java.math.BigDecimal;

/**
 * Result of a reservation. Carries `price` so order-service needs no second round
 * trip — one call, one decision.
 */
public record ReservationResponse(
        Long productId,
        String name,
        BigDecimal price,
        int remainingStock) {

    public static ReservationResponse from(Product product) {
        return new ReservationResponse(
                product.id(),
                product.name(),
                product.price().amount(),
                product.stock());
    }
}
