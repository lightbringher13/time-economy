CREATE TABLE email_change_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,

    old_email VARCHAR(255) NOT NULL,
    new_email VARCHAR(255) NOT NULL,

    -- Second factor strategy decided AFTER new-email verification
    -- PHONE or OLD_EMAIL
    second_factor_type VARCHAR(20),

    -- Workflow state
    -- PENDING
    -- NEW_EMAIL_VERIFIED
    -- SECOND_FACTOR_PENDING
    -- READY_TO_COMMIT
    -- COMPLETED
    -- CANCELED
    -- EXPIRED
    status VARCHAR(30) NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0
);

-- Fast lookup
CREATE INDEX idx_email_change_requests_user_status
    ON email_change_requests (user_id, status);

-- Only one active request per user
CREATE UNIQUE INDEX uq_email_change_requests_active
    ON email_change_requests (user_id)
    WHERE status IN (
        'PENDING',
        'NEW_EMAIL_VERIFIED',
        'SECOND_FACTOR_PENDING',
        'READY_TO_COMMIT'
    );