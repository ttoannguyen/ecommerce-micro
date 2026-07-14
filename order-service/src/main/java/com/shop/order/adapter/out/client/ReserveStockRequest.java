package com.shop.order.adapter.out.client;

/** Wire format of product-service's reservation endpoint. Public for the same reason
 *  as ReservationResponse: the Feign proxy cannot see package-private types. */
public record ReserveStockRequest(int quantity) {
}
