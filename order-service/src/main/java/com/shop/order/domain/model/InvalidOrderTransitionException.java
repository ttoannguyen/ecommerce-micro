package com.shop.order.domain.model;

public class InvalidOrderTransitionException extends IllegalStateException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
