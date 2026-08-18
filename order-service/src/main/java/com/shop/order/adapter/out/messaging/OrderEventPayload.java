package com.shop.order.adapter.out.messaging;

import com.shop.order.domain.model.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderEventPayload(
        Long orderId,
        String status,
        BigDecimal totalPrice,
        String idempotencyKey,
        List<Item> items) {

    public record Item(
            Long productId,
            UUID reservationId,
            String name,
            BigDecimal unitPrice,
            int quantity) {
    }

    public static OrderEventPayload from(Order order) {
        return new OrderEventPayload(order.id(), order.status().name(),
                order.totalPrice().amount(), order.idempotencyKey(),
                order.items().stream().map(item -> new Item(item.productId(),
                        item.reservationId(), item.name(), item.unitPrice().amount(),
                        item.quantity().value())).toList());
    }
}
