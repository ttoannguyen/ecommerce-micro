package com.shop.product.domain.port.in;

import com.shop.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

/** Inbound port: đọc catalog. */
public interface FindProductsUseCase {

    List<Product> findAll();

    Optional<Product> findById(Long id);
}
