package com.shop.product.domain.port.out;

/**
 * Outbound port: reads the ledger itself rather than the projection.
 *
 * Exists so the invariant `balanceOf(id) == product.onHand` can be checked — by a test,
 * by an audit endpoint, or by a monitoring probe. A projection nobody ever reconciles
 * against its source is just a number that used to be right.
 */
public interface LoadStockLedgerPort {

    /** SUM of every signed movement for this product. */
    int balanceOf(Long productId);
}
