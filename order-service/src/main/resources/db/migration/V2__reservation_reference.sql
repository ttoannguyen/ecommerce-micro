-- Existing rows predate reservation identity, so these columns stay nullable for
-- migration compatibility. Every new order writes both values.
ALTER TABLE orders ADD COLUMN reservation_id UUID;
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128);

ALTER TABLE orders ADD CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key);
