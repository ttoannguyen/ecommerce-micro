package com.shop.order.adapter.out.client;

import java.math.BigDecimal;

/** Wire DTO nhận từ Product Service. Chỉ field cần dùng. */
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private int stock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
