package com.shop.product.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchReserveStockRequest(
        @NotEmpty(message = "items phải có ít nhất một sản phẩm")
        List<@Valid ReserveStockLineRequest> items) {
}
