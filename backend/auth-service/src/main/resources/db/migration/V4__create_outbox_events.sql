-- Outbox pattern table for reliable event publishing
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,

    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,

    event_type      VARCHAR(200) NOT NULL,

    payload         TEXT NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | SENT | FAILED

    occurred_at     TIMESTAMPTZ NOT NULL,
    available_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    attempts        INT NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),

    locked_by       VARCHAR(100),
    locked_at       TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ
);

-- Core query index: publisher pulls pending events that are ready
CREATE INDEX idx_outbox_pending_ready
    ON outbox_events (status, available_at, created_at);

-- Aggregate debugging / tracing (optional but very useful)
CREATE INDEX idx_outbox_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

-- Safety check for status values (optional)
ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_status
    CHECK (status IN ('PENDING','SENT','PROCESSING','FAILED'));