package com.shop.product.domain.model;

/**
 * Why a movement happened. The type fixes the direction, so a RECEIPT can never
 * quietly remove stock and an ISSUE can never add it.
 *
 * Quantity is stored signed rather than as an absolute plus a direction flag: the
 * ledger balance is then a plain SUM, and there is no second field that can disagree
 * with the first.
 */
public enum MovementType {

    /** Stock arriving: opening balance or a supplier delivery. */
    RECEIPT,

    /** Stock leaving: taken off the shelf for an order. */
    ISSUE,

    /** Stock coming back after the caller failed and compensated. */
    RELEASE,

    /** Correction after a physical count. Either direction, and it must say why. */
    ADJUSTMENT;

    boolean allows(int signedQuantity) {
        return switch (this) {
            case RECEIPT, RELEASE -> signedQuantity > 0;
            case ISSUE -> signedQuantity < 0;
            case ADJUSTMENT -> signedQuantity != 0;
        };
    }

    /**
     * Only an adjustment needs a reason. The others carry their own explanation:
     * a receipt is a delivery, an issue is an order. An adjustment is the one case
     * where the system and the shelf disagreed, and that always needs a human answer.
     */
    boolean requiresReason() {
        return this == ADJUSTMENT;
    }
}
