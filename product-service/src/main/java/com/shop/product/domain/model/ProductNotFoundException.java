package com.shop.product.domain.model;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Không tìm thấy product " + productId);
    }
}
