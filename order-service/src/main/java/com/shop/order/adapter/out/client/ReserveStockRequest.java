package com.shop.order.adapter.out.client;

/** Wire format of product-service's reservation endpoint. Not a domain type. */
record ReserveStockRequest(int quantity) {
}
