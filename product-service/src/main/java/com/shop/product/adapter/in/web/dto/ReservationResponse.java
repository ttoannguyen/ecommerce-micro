package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.Reservation;
import com.shop.product.domain.model.ReservationDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Result of a reservation. Carries `price` so order-service needs no second round
 * trip — one call, one decision.
 */
public record ReservationResponse(
        UUID reservationId,
        Long productId,
        String name,
        BigDecimal price,
        int quantity,
        String status,
        Instant createdAt,
        Instant expiresAt,
        int onHand,
        int reserved,
        int available) {

    public static ReservationResponse from(ReservationDetails details) {
        Reservation reservation = details.reservation();
        Product product = details.product();
        return new ReservationResponse(
                reservation.id(),
                product.id(),
                product.name(),
                product.price().amount(),
                reservation.quantity(),
                reservation.status().name(),
                reservation.createdAt(),
                reservation.expiresAt(),
                product.onHand(),
                product.reserved(),
                product.available());
    }
}
