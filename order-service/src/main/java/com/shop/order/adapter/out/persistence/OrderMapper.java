package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderItem;
import com.shop.order.domain.model.OrderStatus;
import com.shop.order.domain.model.OrderTransition;
import com.shop.order.domain.model.Quantity;

/** Translates between the domain aggregate and the JPA record. */
final class OrderMapper {

    private OrderMapper() {
    }

    static OrderJpaEntity toEntity(Order order) {
        return new OrderJpaEntity(
                order.id(),
                order.productId(),
                order.reservationId(),
                order.idempotencyKey(),
                order.quantity().value(),
                order.totalPrice().amount(),
                order.status().name(),
                order.createdAt());
    }

    static Order toDomain(OrderJpaEntity entity) {
        return Order.rehydrate(
                entity.getId(),
                java.util.List.of(new OrderItem(entity.getProductId(), entity.getReservationId(),
                        "legacy", Money.of(entity.getTotalPrice()), Quantity.of(entity.getQuantity()))),
                entity.getIdempotencyKey(),
                Money.of(entity.getTotalPrice()),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }

    static Order toDomain(OrderJpaEntity entity, java.util.List<OrderItemJpaEntity> itemEntities) {
        return Order.rehydrate(entity.getId(), itemEntities.stream().map(item -> new OrderItem(
                item.getProductId(), item.getReservationId(), item.getProductName(),
                Money.of(item.getUnitPrice()), Quantity.of(item.getQuantity()))).toList(),
                entity.getIdempotencyKey(), Money.of(entity.getTotalPrice()),
                OrderStatus.valueOf(entity.getStatus()), entity.getCreatedAt());
    }

    static Order toDomain(OrderJpaEntity entity,
                          java.util.List<OrderItemJpaEntity> itemEntities,
                          java.util.List<OrderStatusHistoryJpaEntity> historyEntities) {
        Order fallback = toDomain(entity, itemEntities);
        java.util.List<OrderTransition> history = historyEntities.stream()
                .map(OrderMapper::toTransition).toList();
        return Order.rehydrate(entity.getId(), fallback.items(), entity.getIdempotencyKey(),
                Money.of(entity.getTotalPrice()), OrderStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(), history.isEmpty() ? fallback.history() : history);
    }

    static OrderItemJpaEntity toItemEntity(Long orderId, int lineNumber, OrderItem item) {
        return new OrderItemJpaEntity(null, orderId, lineNumber, item.productId(),
                item.reservationId(), item.name(), item.unitPrice().amount(),
                item.quantity().value());
    }

    static OrderStatusHistoryJpaEntity toHistoryEntity(Long orderId, OrderTransition transition) {
        return new OrderStatusHistoryJpaEntity(null, orderId, transition.sequence(),
                transition.fromStatus(), transition.toStatus(), transition.transitionedAt());
    }

    static OrderTransition toTransition(OrderStatusHistoryJpaEntity entity) {
        return new OrderTransition(entity.getSequence(), entity.getFromStatus(),
                entity.getToStatus(), entity.getTransitionedAt());
    }
}
