package com.shop.order.domain.port.in;

/** Ý định đặt hàng đến từ tầng ngoài. */
public record PlaceOrderCommand(Long productId, int quantity) {
}
