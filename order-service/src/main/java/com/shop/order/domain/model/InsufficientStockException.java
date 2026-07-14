package com.shop.order.domain.model;

/** product-service refused the reservation: not enough stock. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
