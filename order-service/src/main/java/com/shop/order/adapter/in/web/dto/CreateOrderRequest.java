package com.shop.order.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Payload HTTP tạo đơn. Validate ở biên, trước khi vào use case. */
public record CreateOrderRequest(

        @NotNull(message = "productId là bắt buộc")
        Long productId,

        @Positive(message = "quantity phải > 0")
        int quantity) {
}
