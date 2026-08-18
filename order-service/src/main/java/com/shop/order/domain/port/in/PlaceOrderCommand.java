package com.shop.order.domain.port.in;

import com.shop.order.domain.model.OrderItemDraft;
import com.shop.order.domain.model.Quantity;

import java.util.List;

/** The intent to place an order, arriving from the outside. */
public record PlaceOrderCommand(List<OrderItemDraft> items, String idempotencyKey) {
    public PlaceOrderCommand(Long productId, int quantity, String idempotencyKey) {
        this(List.of(new OrderItemDraft(productId, Quantity.of(quantity))), idempotencyKey);
    }
}
