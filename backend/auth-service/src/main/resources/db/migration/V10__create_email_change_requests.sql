-- V10__create_email_change_requests.sql

-- Table to manage the multi-step "change email" workflow
-- BigCom-style: supports new email verification + second factor (phone / old email)
CREATE TABLE email_change_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,

    old_email VARCHAR(255) NOT NULL,
    new_email VARCHAR(255) NOT NULL,

    -- Code sent to the NEW email (prove ownership of new address)
    new_email_code VARCHAR(10) NOT NULL,

    -- What we use as the second factor: PHONE or OLD_EMAIL
    second_factor_type VARCHAR(20),  -- e.g. 'PHONE', 'OLD_EMAIL'

    -- Code sent to phone (SMS) OR old email, depending on second_factor_type
    second_factor_code VARCHAR(10),

    -- Workflow state:
    -- PENDING, NEW_EMAIL_VERIFIED, READY_TO_COMMIT, COMPLETED, CANCELED, EXPIRED
    status VARCHAR(30) NOT NULL,

    -- Overall TTL for this change request
    expires_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- For optimistic locking / race-safety in the domain layer
    version BIGINT NOT NULL DEFAULT 0
);

-- Optional: add a foreign key if your users table is named "users"
-- ALTER TABLE email_change_requests
--     ADD CONSTRAINT fk_email_change_requests_user
--     FOREIGN KEY (user_id) REFERENCES users (id);

-- Fast lookup by user and status (e.g. to find active requests)
CREATE INDEX idx_email_change_requests_user_status
    ON email_change_requests (user_id, status);

-- Ensure only one active request per user
-- Active = in progress (not completed/canceled/expired)
CREATE UNIQUE INDEX uq_email_change_requests_active
    ON email_change_requests (user_id)
    WHERE status IN ('PENDING', 'NEW_EMAIL_VERIFIED', 'READY_TO_COMMIT');