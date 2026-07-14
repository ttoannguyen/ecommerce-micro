package com.shop.order.domain.port.in;

/** The intent to place an order, arriving from the outside. */
public record PlaceOrderCommand(Long productId, int quantity) {
}
