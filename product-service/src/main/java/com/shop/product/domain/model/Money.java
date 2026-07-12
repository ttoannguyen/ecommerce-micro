package com.shop.product.domain.model;

import java.math.BigDecimal;

/** Value object: tiền. Bất biến, không âm. */
public final class Money {

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount không được âm");
        }
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public BigDecimal amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
