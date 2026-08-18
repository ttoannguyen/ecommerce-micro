package com.shop.order.adapter.in.web.dto;

import com.shop.order.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(Long productId, UUID reservationId, String name,
                                BigDecimal unitPrice, int quantity,
                                BigDecimal totalPrice) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.productId(), item.reservationId(), item.name(),
                item.unitPrice().amount(), item.quantity().value(), item.totalPrice().amount());
    }
}
