package com.shop.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("tạo mới -> chưa có id")
    void createLeavesIdUnassigned() {
        Product product = Product.create("Bàn phím cơ", Money.of(new BigDecimal("1200000")), 10);

        assertThat(product.id()).isNull();
        assertThat(product.stock()).isEqualTo(10);
    }

    @Test
    @DisplayName("stock 0 -> hợp lệ (hết hàng khác với sai dữ liệu)")
    void allowsZeroStock() {
        assertThat(Product.create("Hết hàng", Money.of(BigDecimal.TEN), 0).stock()).isZero();
    }

    @Test
    @DisplayName("stock âm -> chặn")
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> Product.create("Sai", Money.of(BigDecimal.TEN), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stock không được âm");
    }

    @Test
    @DisplayName("rehydrate -> giữ nguyên id từ DB")
    void rehydrateKeepsId() {
        Product product = Product.rehydrate(7L, "Chuột", Money.of(BigDecimal.ONE), 3);

        assertThat(product.id()).isEqualTo(7L);
    }
}
