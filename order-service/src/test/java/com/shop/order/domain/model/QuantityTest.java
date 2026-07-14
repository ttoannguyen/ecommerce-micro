package com.shop.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    @DisplayName("số dương -> hợp lệ")
    void acceptsPositive() {
        assertThat(Quantity.of(1).value()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("0 hoặc âm -> chặn ngay ở VO, không lọt vào Order")
    void rejectsZeroAndNegative(int invalid) {
        assertThatThrownBy(() -> Quantity.of(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity phải > 0");
    }
}
