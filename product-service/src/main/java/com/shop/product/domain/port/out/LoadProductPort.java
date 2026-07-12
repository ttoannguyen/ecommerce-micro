package com.shop.product.domain.port.out;

import com.shop.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

/** Outbound port: đọc product từ store. */
public interface LoadProductPort {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    long count();
}
