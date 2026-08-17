package com.shop.product.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static Product saved(int stock) {
        return Product.rehydrate(1L, "Bàn phím cơ", Money.of(new BigDecimal("1200000")), stock);
    }

    @Test
    @DisplayName("tạo mới -> chưa có id, và stock bắt đầu từ 0")
    void createStartsEmpty() {
        Product product = Product.create("Bàn phím cơ", Money.of(new BigDecimal("1200000")));

        assertThat(product.id()).isNull();
        assertThat(product.stock()).isZero();
    }

    @Test
    @DisplayName("chưa lưu -> không ghi được ledger (movement phải có product id)")
    void refusesLedgerWriteBeforePersist() {
        Product unsaved = Product.create("Chưa lưu", Money.of(BigDecimal.TEN));

        assertThatThrownBy(() -> unsaved.receive(5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("nhập hàng -> stock tăng, sinh movement RECEIPT dương")
    void receiveAddsStock() {
        StockChange change = saved(0).receive(10);

        assertThat(change.product().stock()).isEqualTo(10);
        assertThat(change.movement().type()).isEqualTo(MovementType.RECEIPT);
        assertThat(change.movement().quantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("reserve -> stock giảm, movement ISSUE mang dấu âm")
    void reserveIssuesNegativeMovement() {
        StockChange change = saved(10).reserve(3);

        assertThat(change.product().stock()).isEqualTo(7);
        assertThat(change.movement().type()).isEqualTo(MovementType.ISSUE);
        assertThat(change.movement().quantity()).isEqualTo(-3);
    }

    @Test
    @DisplayName("reserve quá tồn -> chặn, không sinh movement nào")
    void reserveRefusesWhenShort() {
        assertThatThrownBy(() -> saved(2).reserve(3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Còn 2, cần 3");
    }

    @Test
    @DisplayName("release -> trả hàng về, movement RELEASE dương")
    void releaseReturnsStock() {
        StockChange change = saved(7).release(3);

        assertThat(change.product().stock()).isEqualTo(10);
        assertThat(change.movement().type()).isEqualTo(MovementType.RELEASE);
        assertThat(change.movement().quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("điều chỉnh giảm -> ghi nhận chênh lệch kèm lý do, không nuốt mất")
    void adjustRecordsDiscrepancy() {
        StockChange change = saved(10).adjust(-2, ReasonCode.CYCLE_COUNT);

        assertThat(change.product().stock()).isEqualTo(8);
        assertThat(change.movement().type()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(change.movement().quantity()).isEqualTo(-2);
        assertThat(change.movement().reason()).isEqualTo(ReasonCode.CYCLE_COUNT);
    }

    @Test
    @DisplayName("điều chỉnh 0 -> không phải sự kiện, chặn")
    void rejectsZeroAdjustment() {
        assertThatThrownBy(() -> saved(10).adjust(0, ReasonCode.CORRECTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("quantity <= 0 -> chặn ở mọi cửa")
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> saved(10).reserve(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity phải > 0");
        assertThatThrownBy(() -> saved(10).receive(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity phải > 0");
    }

    @Test
    @DisplayName("stock âm -> chặn ngay ở constructor")
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> Product.rehydrate(1L, "Sai", Money.of(BigDecimal.TEN), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stock không được âm");
    }

    @Test
    @DisplayName("rehydrate -> giữ nguyên id từ DB")
    void rehydrateKeepsId() {
        assertThat(Product.rehydrate(7L, "Chuột", Money.of(BigDecimal.ONE), 3).id()).isEqualTo(7L);
    }

    @Test
    @DisplayName("chuỗi thao tác -> stock luôn bằng tổng các movement")
    void stockAlwaysEqualsLedgerSum() {
        StockChange received = saved(0).receive(10);
        StockChange reserved = received.product().reserve(4);
        StockChange released = reserved.product().release(1);
        StockChange adjusted = released.product().adjust(-2, ReasonCode.DAMAGE);

        int ledger = received.movement().quantity()
                + reserved.movement().quantity()
                + released.movement().quantity()
                + adjusted.movement().quantity();

        assertThat(adjusted.product().stock()).isEqualTo(ledger).isEqualTo(5);
    }
}
