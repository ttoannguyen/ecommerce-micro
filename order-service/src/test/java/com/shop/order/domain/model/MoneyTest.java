package com.shop.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("nhân số lượng -> đúng tổng")
    void multipliesByFactor() {
        assertThat(Money.of(new BigDecimal("1200000")).multiply(3))
                .isEqualTo(Money.of(new BigDecimal("3600000")));
    }

    @Test
    @DisplayName("so sánh theo giá trị, không theo scale: 100 == 100.00")
    void comparesByValueNotScale() {
        assertThat(Money.of(new BigDecimal("100")))
                .isEqualTo(Money.of(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("âm -> chặn")
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount không được âm");
    }

    @Test
    @DisplayName("null -> chặn")
    void rejectsNull() {
        assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount không được null");
    }
}
