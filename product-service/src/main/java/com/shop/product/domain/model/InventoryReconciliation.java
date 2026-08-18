package com.shop.product.domain.model;

public record InventoryReconciliation(
        Long productId,
        int expectedOnHand,
        int actualOnHand,
        int expectedReserved,
        int actualReserved) {

    public boolean consistent() {
        return expectedOnHand == actualOnHand && expectedReserved == actualReserved;
    }
}
