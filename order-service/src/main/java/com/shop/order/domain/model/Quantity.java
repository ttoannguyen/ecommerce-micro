package com.shop.order.domain.model;

/** Value object: số lượng đặt. Luôn > 0. */
public final class Quantity {

    private final int value;

    private Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public int value() {
        return value;
    }
}
