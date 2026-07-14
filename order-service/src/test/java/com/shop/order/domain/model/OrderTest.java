package com.shop.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order no longer checks stock, so there is nothing here about stock. It checks the
 * one thing it actually owns: the arithmetic. Stock invariants moved to
 * product-service, whose ProductTest covers them.
 */
class OrderTest {

    private static ReservedProduct reserved(String price) {
        return new ReservedProduct(1L, "Mechanical keyboard", Money.of(new BigDecimal(price)));
    }

    @Test
    @DisplayName("total = unit price x quantity")
    void computesTotalFromReservedPrice() {
        Order order = Order.place(reserved("1200000"), Quantity.of(3));

        assertThat(order.id()).isNull();
        assertThat(order.productId()).isEqualTo(1L);
        assertThat(order.quantity().value()).isEqualTo(3);
        assertThat(order.totalPrice()).isEqualTo(Money.of(new BigDecimal("3600000")));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("a new order starts as CREATED")
    void startsInCreatedStatus() {
        assertThat(Order.place(reserved("1000"), Quantity.of(1)).status())
                .isEqualTo(OrderStatus.CREATED);
    }
}
