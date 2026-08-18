package com.shop.product.domain.model;

public class InvalidReservationTransitionException extends RuntimeException {
    public InvalidReservationTransitionException(String message) {
        super(message);
    }
}
