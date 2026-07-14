package com.shop.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test bất biến của aggregate. Không @SpringBootTest, không DB, không mock —
 * Order là POJO thuần nên chạy trong vài mili giây. Đây là phần thưởng của hexagonal.
 */
class OrderTest {

    private static ProductSnapshot product(int stock, String price) {
        return new ProductSnapshot(1L, "Bàn phím cơ", Money.of(new BigDecimal(price)), stock);
    }

    @Test
    @DisplayName("đủ tồn kho -> tạo đơn, tổng tiền = đơn giá x số lượng")
    void placesOrderWhenStockIsEnough() {
        Order order = Order.place(product(10, "1200000"), Quantity.of(3));

        assertThat(order.id()).isNull();
        assertThat(order.productId()).isEqualTo(1L);
        assertThat(order.quantity().value()).isEqualTo(3);
        assertThat(order.totalPrice()).isEqualTo(Money.of(new BigDecimal("3600000")));
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("mua đúng bằng tồn kho -> vẫn hợp lệ (biên)")
    void allowsBuyingExactlyAllStock() {
        Order order = Order.place(product(5, "1000"), Quantity.of(5));

        assertThat(order.quantity().value()).isEqualTo(5);
    }

    @Test
    @DisplayName("thiếu tồn kho -> InsufficientStockException, không tạo đơn")
    void rejectsOrderWhenStockIsInsufficient() {
        assertThatThrownBy(() -> Order.place(product(2, "1000"), Quantity.of(3)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Còn 2")
                .hasMessageContaining("cần 3");
    }

    @Test
    @DisplayName("hết hàng -> mọi số lượng đều bị chặn")
    void rejectsOrderWhenOutOfStock() {
        assertThatThrownBy(() -> Order.place(product(0, "1000"), Quantity.of(1)))
                .isInstanceOf(InsufficientStockException.class);
    }
}
