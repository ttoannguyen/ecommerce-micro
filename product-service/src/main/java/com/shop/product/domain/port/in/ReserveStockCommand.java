package com.shop.product.domain.port.in;

public record ReserveStockCommand(Long productId, int quantity) {
}
