package com.shop.order.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchCreateOrderRequest(
        @NotEmpty(message = "items phải có ít nhất một sản phẩm")
        List<@Valid CreateOrderItemRequest> items) {
}
