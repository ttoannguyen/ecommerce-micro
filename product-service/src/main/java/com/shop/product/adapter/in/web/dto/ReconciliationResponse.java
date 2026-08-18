package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.InventoryReconciliation;

public record ReconciliationResponse(
        Long productId,
        int expectedOnHand,
        int actualOnHand,
        int expectedReserved,
        int actualReserved,
        boolean consistent) {

    public static ReconciliationResponse from(InventoryReconciliation result) {
        return new ReconciliationResponse(
                result.productId(), result.expectedOnHand(), result.actualOnHand(),
                result.expectedReserved(), result.actualReserved(), result.consistent());
    }
}
