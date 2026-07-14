package com.shop.product.application;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.port.in.ReserveStockCommand;
import com.shop.product.domain.port.in.ReserveStockUseCase;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read, check and decrement happen inside ONE transaction, on a row held by a write
 * lock. order-service cannot do this over REST: between the moment it reads the stock
 * and the moment it saves the order there is always a gap for someone else to slip in.
 *
 * Pessimistic rather than optimistic, because stock is a single hot row. Optimistic
 * locking would let one writer win and make the other nineteen fail — no oversell, but
 * a lot of spurious 409s. A write lock makes them queue instead of collide.
 */
@Service
public class ReserveStockService implements ReserveStockUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    public ReserveStockService(LoadProductPort loadProductPort, SaveProductPort saveProductPort) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
    }

    @Override
    @Transactional
    public Product reserve(ReserveStockCommand command) {
        Product product = load(command.productId());
        return saveProductPort.save(product.reserve(command.quantity()));
    }

    @Override
    @Transactional
    public Product release(ReserveStockCommand command) {
        Product product = load(command.productId());
        return saveProductPort.save(product.release(command.quantity()));
    }

    private Product load(Long productId) {
        return loadProductPort.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
