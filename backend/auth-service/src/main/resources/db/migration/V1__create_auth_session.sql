-- ============================================================
-- V1__create_auth_session.sql
-- Create table for storing refresh token sessions
-- ============================================================

CREATE TABLE auth_session (
    id BIGSERIAL PRIMARY KEY,

    -- User identity comes from user-service
    user_id BIGINT NOT NULL,

    -- Device family (one per browser install)
    family_id VARCHAR(100) NOT NULL,

    -- Refresh token hash
    token_hash VARCHAR(255) NOT NULL,

    -- Device / environment info
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,

    -- State flags
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    reuse_detected BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- Indexes for performance
-- ============================================================

-- Search by token hash (most common)
CREATE INDEX idx_auth_session_token_hash
    ON auth_session (token_hash);

-- Search by family
CREATE INDEX idx_auth_session_family
    ON auth_session (family_id);

-- Search by user
CREATE INDEX idx_auth_session_user
    ON auth_session (user_id);

-- Filter by expiration
CREATE INDEX idx_auth_session_expires
    ON auth_session (expires_at);

-- Filter by revoked
CREATE INDEX idx_auth_session_revoked
    ON auth_session (revoked);