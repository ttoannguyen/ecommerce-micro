package com.shop.product.domain.port.out;

import com.shop.product.domain.model.Product;

/** Outbound port: saves a product. */
public interface SaveProductPort {

    Product save(Product product);
}
