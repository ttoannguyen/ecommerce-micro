ALTER TABLE reservation DROP CONSTRAINT uq_reservation_caller_key;

ALTER TABLE reservation ADD CONSTRAINT uq_reservation_caller_key_product
    UNIQUE (caller, idempotency_key, product_id);
