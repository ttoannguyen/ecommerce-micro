package com.shop.product.domain.port.in;

import com.shop.product.domain.model.InventoryReconciliation;

public interface ReconcileInventoryUseCase {
    InventoryReconciliation reconcile(Long productId);
}
