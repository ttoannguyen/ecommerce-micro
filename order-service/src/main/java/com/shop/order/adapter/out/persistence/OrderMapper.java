package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderStatus;
import com.shop.order.domain.model.Quantity;

/** Translates between the domain aggregate and the JPA record. */
final class OrderMapper {

    private OrderMapper() {
    }

    static OrderJpaEntity toEntity(Order order) {
        return new OrderJpaEntity(
                order.id(),
                order.productId(),
                order.quantity().value(),
                order.totalPrice().amount(),
                order.status().name(),
                order.createdAt());
    }

    static Order toDomain(OrderJpaEntity entity) {
        return Order.rehydrate(
                entity.getId(),
                entity.getProductId(),
                Quantity.of(entity.getQuantity()),
                Money.of(entity.getTotalPrice()),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
