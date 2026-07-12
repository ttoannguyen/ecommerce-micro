package com.shop.order.domain.model;

/**
 * Ảnh chiếu Product trong bounded context Order (anti-corruption).
 * Order không sở hữu Product — chỉ giữ dữ liệu cần để đặt hàng.
 */
public final class ProductSnapshot {

    private final Long productId;
    private final String name;
    private final Money price;
    private final int stock;

    public ProductSnapshot(Long productId, String name, Money price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public boolean hasStockFor(Quantity quantity) {
        return stock >= quantity.value();
    }

    public Long productId() {
        return productId;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }

    public int stock() {
        return stock;
    }
}
