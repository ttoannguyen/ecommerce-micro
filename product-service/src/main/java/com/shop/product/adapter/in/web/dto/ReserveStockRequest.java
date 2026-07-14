package com.shop.product.adapter.in.web.dto;

import jakarta.validation.constraints.Positive;

public record ReserveStockRequest(

        @Positive(message = "quantity phải > 0")
        int quantity) {
}
