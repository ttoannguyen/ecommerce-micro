package com.shop.order.domain.model;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Không tìm thấy order " + id);
    }
}
