package com.shop.order.domain.model;

/** Lifecycle status of an order. */
public enum OrderStatus {
    PENDING_RESERVATION,
    RESERVED,
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CANCELLED,
    FAILED
}
