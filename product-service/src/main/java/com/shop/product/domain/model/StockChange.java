package com.shop.product.domain.model;

/**
 * The new state of the product and the ledger line that explains it, together.
 *
 * They travel as one value on purpose. Writing the projection without the movement
 * gives a number nobody can justify; writing the movement without the projection
 * gives a balance that disagrees with the row everyone reads. The port takes this
 * pair, so there is no signature that lets a caller write only half.
 */
public record StockChange(Product product, StockMovement movement) {
}
