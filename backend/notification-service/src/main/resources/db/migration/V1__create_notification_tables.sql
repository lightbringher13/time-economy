-- V1__create_notification_tables.sql
-- Notification service: inbox(idempotency) + delivery history

-- =========================================================
-- 1) Idempotency / inbox (per consumer group)
-- =========================================================
CREATE TABLE processed_events (
    id             BIGSERIAL PRIMARY KEY,

    consumer_group VARCHAR(200) NOT NULL,

    -- best: outbox UUID
    event_id       UUID NOT NULL,
    event_type     VARCHAR(200) NOT NULL,

    topic          VARCHAR(300) NOT NULL,
    kafka_partition INT NOT NULL,
    kafka_offset   BIGINT NOT NULL,

    processed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_processed_events UNIQUE (consumer_group, event_id)
);

CREATE INDEX idx_processed_events_group_processed_at
    ON processed_events (consumer_group, processed_at DESC);

CREATE INDEX idx_processed_events_topic_partition_offset
    ON processed_events (topic, kafka_partition, kafka_offset);


-- =========================================================
-- 2) Delivery history / audit / debugging
-- =========================================================
CREATE TABLE notification_deliveries (
    id              BIGSERIAL PRIMARY KEY,

    event_id         UUID NOT NULL,
    event_type       VARCHAR(200) NOT NULL,

    channel          VARCHAR(30)  NOT NULL,        -- EMAIL | SMS | PUSH
    template         VARCHAR(100) NOT NULL,        -- WELCOME_EMAIL, EMAIL_CHANGED, etc.

    recipient        VARCHAR(320) NOT NULL,        -- consider hash if you want less PII

    status           VARCHAR(20)  NOT NULL,        -- SENT | FAILED
    provider         VARCHAR(50),                  -- brevo, ses, sendgrid...
    provider_msg_id  VARCHAR(200),

    error_message    VARCHAR(500),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_notification_status
      CHECK (status IN ('SENT','FAILED')),

    CONSTRAINT chk_notification_channel
      CHECK (channel IN ('EMAIL','SMS','PUSH')),

    -- ✅ prevent duplicate delivery effects (choose your strictness)
    CONSTRAINT uq_notification_delivery UNIQUE (event_id, template)
);

CREATE INDEX idx_notification_deliveries_event
    ON notification_deliveries (event_id, event_type);

CREATE INDEX idx_notification_deliveries_created_at
    ON notification_deliveries (created_at DESC);