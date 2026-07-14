package com.shop.product.domain.model;

/** Not enough stock to reserve. Thrown by the aggregate that owns the stock. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
