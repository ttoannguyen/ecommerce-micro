package com.shop.product.domain.port.out;

import com.shop.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

/** Outbound port: reads products from the store. */
public interface LoadProductPort {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    /**
     * Loads a product with the intent to modify it. Concurrent callers must queue up
     * rather than all read the same stock and all conclude there is enough.
     *
     * The port states the intent; the adapter picks the mechanism (here: SELECT ...
     * FOR UPDATE). The domain never learns the word "lock".
     */
    Optional<Product> findByIdForUpdate(Long id);

    long count();
}
