package com.shop.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order no longer checks stock, so there is nothing here about stock. It checks the
 * one thing it actually owns: the arithmetic. Stock invariants moved to
 * product-service, whose ProductTest covers them.
 */
class OrderTest {

    private static final UUID RESERVATION_ID = UUID.randomUUID();

    private static ReservedProduct reserved(String price) {
        return new ReservedProduct(1L, RESERVATION_ID, "Mechanical keyboard",
                Money.of(new BigDecimal(price)), Instant.now().plusSeconds(900));
    }

    @Test
    @DisplayName("total = unit price x quantity")
    void computesTotalFromReservedPrice() {
        Order order = Order.place(reserved("1200000"), Quantity.of(3), "order-key");

        assertThat(order.id()).isNull();
        assertThat(order.productId()).isEqualTo(1L);
        assertThat(order.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(order.idempotencyKey()).isEqualTo("order-key");
        assertThat(order.quantity().value()).isEqualTo(3);
        assertThat(order.totalPrice()).isEqualTo(Money.of(new BigDecimal("3600000")));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("a new order starts as CREATED")
    void startsInCreatedStatus() {
        assertThat(Order.place(reserved("1000"), Quantity.of(1), "status-key").status())
                .isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("batch order snapshots every reserved line and starts RESERVED")
    void batchOrderSnapshotsLines() {
        Order order = Order.place(List.of(
                new ReservedProduct(1L, UUID.randomUUID(), "Keyboard",
                        Money.of(new BigDecimal("1000")), 2, Instant.now().plusSeconds(900)),
                new ReservedProduct(2L, UUID.randomUUID(), "Mouse",
                        Money.of(new BigDecimal("500")), 3, Instant.now().plusSeconds(900))),
                "batch-key");

        assertThat(order.items()).hasSize(2);
        assertThat(order.totalPrice()).isEqualTo(Money.of(new BigDecimal("3500")));
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    @DisplayName("order state machine rejects payment after cancellation")
    void rejectsInvalidTransition() {
        Order order = Order.place(reserved("1000"), Quantity.of(1), "transition-key")
                .transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(IllegalStateException.class);
    }
}
