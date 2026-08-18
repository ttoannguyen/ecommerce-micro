package com.shop.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static Product saved(int onHand, int reserved) {
        return Product.rehydrate(1L, "Bàn phím cơ",
                Money.of(new BigDecimal("1200000")), onHand, reserved);
    }

    @Test
    @DisplayName("sản phẩm mới bắt đầu với on-hand/reserved/available bằng 0")
    void createStartsEmpty() {
        Product product = Product.create("Bàn phím cơ", Money.of(BigDecimal.TEN));

        assertThat(product.onHand()).isZero();
        assertThat(product.reserved()).isZero();
        assertThat(product.available()).isZero();
    }

    @Test
    @DisplayName("receipt tăng on-hand và ghi movement dương")
    void receiveChangesPhysicalInventory() {
        StockChange change = saved(0, 0).receive(10);

        assertThat(change.product().onHand()).isEqualTo(10);
        assertThat(change.product().reserved()).isZero();
        assertThat(change.movement().type()).isEqualTo(MovementType.RECEIPT);
        assertThat(change.movement().quantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("hold chỉ tăng reserved, không giảm on-hand")
    void holdOnlyChangesAllocation() {
        Product held = saved(10, 0).hold(3);

        assertThat(held.onHand()).isEqualTo(10);
        assertThat(held.reserved()).isEqualTo(3);
        assertThat(held.available()).isEqualTo(7);
    }

    @Test
    @DisplayName("hold kiểm tra available thay vì on-hand")
    void holdRefusesWhenAvailableIsShort() {
        assertThatThrownBy(() -> saved(10, 8).hold(3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Còn 2, cần 3");
    }

    @Test
    @DisplayName("release hold chỉ giảm reserved")
    void releaseOnlyChangesAllocation() {
        Product released = saved(10, 4).releaseHold(3);

        assertThat(released.onHand()).isEqualTo(10);
        assertThat(released.reserved()).isEqualTo(1);
        assertThat(released.available()).isEqualTo(9);
    }

    @Test
    @DisplayName("fulfillment giảm on-hand và reserved, ghi ISSUE")
    void fulfillmentCreatesPhysicalIssue() {
        StockChange fulfilled = saved(10, 4).fulfill(3);

        assertThat(fulfilled.product().onHand()).isEqualTo(7);
        assertThat(fulfilled.product().reserved()).isEqualTo(1);
        assertThat(fulfilled.movement().type()).isEqualTo(MovementType.ISSUE);
        assertThat(fulfilled.movement().quantity()).isEqualTo(-3);
    }

    @Test
    @DisplayName("không thể release hoặc fulfill nhiều hơn reserved")
    void cannotConsumeMissingReservation() {
        assertThatThrownBy(() -> saved(10, 2).releaseHold(3))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> saved(10, 2).fulfill(3))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reserved luôn nằm trong 0..onHand")
    void validatesBalanceInvariant() {
        assertThatThrownBy(() -> Product.rehydrate(
                1L, "Sai", Money.of(BigDecimal.TEN), 2, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> saved(10, 8).adjust(-3, ReasonCode.DAMAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("adjustment thay đổi on-hand và bắt buộc có ledger reason")
    void adjustmentKeepsLedgerExplainable() {
        StockChange change = saved(10, 2).adjust(-2, ReasonCode.CYCLE_COUNT);

        assertThat(change.product().onHand()).isEqualTo(8);
        assertThat(change.product().reserved()).isEqualTo(2);
        assertThat(change.movement().type()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(change.movement().quantity()).isEqualTo(-2);
    }
}
