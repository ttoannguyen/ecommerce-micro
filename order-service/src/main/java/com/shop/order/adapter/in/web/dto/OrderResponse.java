package com.shop.order.adapter.in.web.dto;

import com.shop.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;

/** HTTP view of an order. Keeps the aggregate away from the wire format. */
public record OrderResponse(
        Long id,
        Long productId,
        int quantity,
        BigDecimal totalPrice,
        String status,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.productId(),
                order.quantity().value(),
                order.totalPrice().amount(),
                order.status().name(),
                order.createdAt());
    }
}
