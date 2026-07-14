package com.shop.product.domain.port.in;

import com.shop.product.domain.model.Product;

public interface ReserveStockUseCase {

    /** Takes stock off the shelf. Throws InsufficientStockException when short. */
    Product reserve(ReserveStockCommand command);

    /** Puts stock back (saga compensation). */
    Product release(ReserveStockCommand command);
}
