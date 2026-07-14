package com.shop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** HTTP payload for creating an order. Validated at the boundary, before the use case. */
public record CreateOrderRequest(

        @NotNull(message = "productId là bắt buộc")
        Long productId,

        @Positive(message = "quantity phải > 0")
        int quantity) {
}
