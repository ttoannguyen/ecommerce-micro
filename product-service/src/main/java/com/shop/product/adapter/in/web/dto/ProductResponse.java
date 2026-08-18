package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.Product;

import java.math.BigDecimal;

/** HTTP view of a product. */
public record ProductResponse(Long id, String name, BigDecimal price,
                              int onHand, int reserved, int available) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price().amount(),
                product.onHand(),
                product.reserved(),
                product.available());
    }
}
