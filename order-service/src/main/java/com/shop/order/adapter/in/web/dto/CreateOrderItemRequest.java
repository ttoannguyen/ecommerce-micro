package com.shop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull(message = "productId là bắt buộc") Long productId,
        @Positive(message = "quantity phải > 0") int quantity) {
}
