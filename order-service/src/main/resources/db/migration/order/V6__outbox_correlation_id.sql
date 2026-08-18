ALTER TABLE outbox_events
    ADD COLUMN correlation_id VARCHAR(128);

CREATE INDEX ix_outbox_events_correlation_id
    ON outbox_events (correlation_id);
