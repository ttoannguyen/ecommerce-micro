package com.shop.order.domain.model;

/** Vi phạm bất biến miền: không đủ tồn kho để đặt. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
