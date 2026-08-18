package com.shop.product.application;

import com.shop.product.domain.model.InventoryReconciliation;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.port.in.ReconcileInventoryUseCase;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.LoadStockLedgerPort;
import com.shop.product.domain.port.out.ReservationStorePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReconciliationService implements ReconcileInventoryUseCase {

    private final LoadProductPort products;
    private final LoadStockLedgerPort ledger;
    private final ReservationStorePort reservations;

    public InventoryReconciliationService(LoadProductPort products,
                                          LoadStockLedgerPort ledger,
                                          ReservationStorePort reservations) {
        this.products = products;
        this.ledger = ledger;
        this.reservations = reservations;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReconciliation reconcile(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return new InventoryReconciliation(
                productId,
                ledger.balanceOf(productId),
                product.onHand(),
                reservations.heldQuantityFor(productId),
                product.reserved());
    }
}
