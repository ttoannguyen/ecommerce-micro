package com.shop.order.adapter.out.client;

import java.math.BigDecimal;

/** Wire format returned by product-service after it takes the stock. Not a domain type. */
record ReservationResponse(Long productId, String name, BigDecimal price, int remainingStock) {
}
