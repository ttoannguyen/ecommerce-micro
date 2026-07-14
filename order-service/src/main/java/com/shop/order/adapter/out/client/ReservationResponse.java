package com.shop.order.adapter.out.client;

import java.math.BigDecimal;

/**
 * Wire format returned by product-service after it takes the stock. Not a domain type.
 *
 * Must stay public. Feign builds a JDK dynamic proxy for ProductClient, and that proxy
 * lives in the jdk.proxy2 module — a different runtime package — so a package-private
 * type here fails at runtime with IllegalAccessError, not at compile time.
 */
public record ReservationResponse(Long productId, String name, BigDecimal price, int remainingStock) {
}
