package com.shop.product.domain.model;

/** Aggregate root Product — POJO thuần, không annotation framework. */
public class Product {

    private final Long id;
    private final String name;
    private final Money price;
    private final int stock;

    private Product(Long id, String name, Money price, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock không được âm");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** Tạo product mới (chưa có id). */
    public static Product create(String name, Money price, int stock) {
        return new Product(null, name, price, stock);
    }

    /** Dựng lại từ tầng persistence (đã có id). */
    public static Product rehydrate(Long id, String name, Money price, int stock) {
        return new Product(id, name, price, stock);
    }

    public Long id() {
        return id;
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
