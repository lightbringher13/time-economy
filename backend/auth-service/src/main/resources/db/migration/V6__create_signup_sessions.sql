-- Signup session table: tracks in-progress registrations
CREATE TABLE signup_sessions (
    -- Opaque session ID used by FE (UUID generated in backend code)
    id              UUID           PRIMARY KEY,

    -- Identity for this signup
    email           VARCHAR(255)   NOT NULL,

    -- Verification flags
    email_verified  BOOLEAN        NOT NULL DEFAULT FALSE,
    phone_number    VARCHAR(30),
    phone_verified  BOOLEAN        NOT NULL DEFAULT FALSE,

    -- Optional profile fields captured during signup
    name            VARCHAR(100),
    gender          VARCHAR(10),
    birth_date      DATE,

    -- High-level state of the signup flow
    -- e.g. EMAIL_PENDING, EMAIL_VERIFIED, PROFILE_FILLED, COMPLETED, EXPIRED
    state           VARCHAR(30)    NOT NULL DEFAULT 'EMAIL_PENDING',

    -- Lifecycle timestamps
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP      NOT NULL
);

-- (Optional but useful) Index to quickly find active sessions by email
CREATE INDEX idx_signup_sessions_email
    ON signup_sessions (email);

-- (Optional) Index for cleanup queries (delete expired sessions)
CREATE INDEX idx_signup_sessions_expires_at
    ON signup_sessions (expires_at);