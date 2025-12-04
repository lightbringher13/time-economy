-- Email verification codes table
CREATE TABLE email_verifications (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    code         VARCHAR(10)  NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    verified_at  TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index: quickly find latest verification attempt for an email
CREATE INDEX idx_email_verifications_email_created_at
    ON email_verifications (email, created_at DESC);

-- Unique: prevent duplicate same code for same email
CREATE UNIQUE INDEX uq_email_verifications_email_code
    ON email_verifications (email, code);