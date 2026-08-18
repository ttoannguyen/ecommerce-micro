package com.shop.order.domain.model;

public record OrderItemDraft(Long productId, Quantity quantity) {
    public OrderItemDraft {
        if (productId == null || quantity == null) {
            throw new IllegalArgumentException("order item thiếu productId hoặc quantity");
        }
    }
}
