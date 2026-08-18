ALTER TABLE product RENAME COLUMN stock TO on_hand;
ALTER TABLE product ADD COLUMN reserved INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE product ADD CONSTRAINT ck_product_on_hand_nonnegative
    CHECK (on_hand >= 0);
ALTER TABLE product ADD CONSTRAINT ck_product_reserved_valid
    CHECK (reserved >= 0 AND reserved <= on_hand);

CREATE TABLE reservation (
    id              UUID PRIMARY KEY,
    caller          VARCHAR(64)  NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    product_id      BIGINT       NOT NULL REFERENCES product (id),
    quantity        INTEGER      NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version         BIGINT       NOT NULL,
    CONSTRAINT uq_reservation_caller_key UNIQUE (caller, idempotency_key),
    CONSTRAINT ck_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT ck_reservation_status
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_reservation_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_reservation_expiry ON reservation (status, expires_at);
CREATE INDEX idx_reservation_product ON reservation (product_id, status);
