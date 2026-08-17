package com.shop.product.domain.model;

/** Why the shelf and the system disagreed. Required on every ADJUSTMENT. */
public enum ReasonCode {

    /** A physical count came out different from the recorded balance. */
    CYCLE_COUNT,

    /** Goods were broken, expired or otherwise unsellable. */
    DAMAGE,

    /** A previous movement was entered wrongly and is being corrected. */
    CORRECTION
}
