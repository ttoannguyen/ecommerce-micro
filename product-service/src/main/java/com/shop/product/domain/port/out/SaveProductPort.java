package com.shop.product.domain.port.out;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.StockChange;

/** Outbound port: saves a product. */
public interface SaveProductPort {

    /** Creates or renames a product. Never touches the stock — only {@link #apply} does. */
    Product save(Product product);

    /**
     * Writes the new balance and the ledger line that explains it, atomically.
     *
     * One method rather than two, so no caller can write the projection and forget the
     * movement. The adapter is responsible for doing both inside the same transaction.
     */
    Product apply(StockChange change);
}
