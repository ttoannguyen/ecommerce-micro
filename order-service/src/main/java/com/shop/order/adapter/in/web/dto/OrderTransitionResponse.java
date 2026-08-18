package com.shop.order.adapter.in.web.dto;

import com.shop.order.domain.model.OrderTransition;

import java.time.Instant;

public record OrderTransitionResponse(Integer sequence, String fromStatus,
                                      String toStatus, Instant transitionedAt) {
    public static OrderTransitionResponse from(OrderTransition transition) {
        return new OrderTransitionResponse(transition.sequence(),
                transition.fromStatus() == null ? null : transition.fromStatus().name(),
                transition.toStatus().name(), transition.transitionedAt());
    }
}
