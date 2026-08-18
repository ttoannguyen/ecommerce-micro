package com.shop.order.domain.model;

import java.time.Instant;

public record OrderTransition(int sequence, OrderStatus fromStatus,
                              OrderStatus toStatus, Instant transitionedAt) {
}
