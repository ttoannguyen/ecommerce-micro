package com.shop.order.domain.model;

import java.time.Instant;

/**
 * Aggregate root Order — POJO thuần, KHÔNG annotation framework.
 * Bất biến miền (đủ tồn kho, tính tiền) nằm ở đây, không rò ra ngoài.
 */
public class Order {

    private final Long id;
    private final Long productId;
    private final Quantity quantity;
    private final Money totalPrice;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(Long id, Long productId, Quantity quantity,
                  Money totalPrice, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Factory: đặt đơn mới dựa trên ảnh chiếu product. Ép bất biến tồn kho. */
    public static Order place(ProductSnapshot product, Quantity quantity) {
        if (!product.hasStockFor(quantity)) {
            throw new InsufficientStockException(
                    "Không đủ tồn kho. Còn " + product.stock()
                            + ", cần " + quantity.value());
        }
        Money total = product.price().multiply(quantity.value());
        return new Order(null, product.productId(), quantity, total,
                OrderStatus.CREATED, Instant.now());
    }

    /** Dựng lại aggregate từ tầng persistence (đã có id). */
    public static Order rehydrate(Long id, Long productId, Quantity quantity,
                                  Money totalPrice, OrderStatus status, Instant createdAt) {
        return new Order(id, productId, quantity, totalPrice, status, createdAt);
    }

    public Long id() {
        return id;
    }

    public Long productId() {
        return productId;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Money totalPrice() {
        return totalPrice;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
