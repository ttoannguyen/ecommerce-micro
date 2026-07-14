package com.shop.product.domain.model;

/**
 * Product aggregate root — plain POJO, no framework annotations.
 *
 * Product owns the stock, so the "never oversell" invariant is enforced HERE.
 * order-service cannot enforce it: all it ever sees is a copy that is already stale.
 */
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

    /** Creates a new product (no id yet). */
    public static Product create(String name, Money price, int stock) {
        return new Product(null, name, price, stock);
    }

    /** Rebuilds from persistence (id already assigned). */
    public static Product rehydrate(Long id, String name, Money price, int stock) {
        return new Product(id, name, price, stock);
    }

    /** Reserve: take the stock off the shelf now. A decision, not a question. */
    public Product reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (stock < quantity) {
            throw new InsufficientStockException(
                    "Không đủ tồn kho. Còn " + stock + ", cần " + quantity);
        }
        return new Product(id, name, price, stock - quantity);
    }

    /** Compensation: put the stock back after the caller failed. */
    public Product release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        return new Product(id, name, price, stock + quantity);
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
