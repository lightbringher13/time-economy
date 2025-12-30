-- ============================================================
-- Auth Session (Instant-friendly)
-- ============================================================
CREATE TABLE auth_session (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    family_id VARCHAR(100) NOT NULL,

    -- refresh token hash (should be unique)
    token_hash VARCHAR(255) NOT NULL,

    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- ✅ Instant-friendly timestamps (UTC-safe)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,

    -- state flags
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    reuse_detected BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- Indexes
-- ============================================================

-- Fast lookup by token hash (most common)
CREATE UNIQUE INDEX ux_auth_session_token_hash
    ON auth_session (token_hash);

-- Family-level session management / rotation
CREATE INDEX idx_auth_session_family
    ON auth_session (family_id);

-- Query sessions by user
CREATE INDEX idx_auth_session_user
    ON auth_session (user_id);

-- Expiry-based cleanup / queries
CREATE INDEX idx_auth_session_expires
    ON auth_session (expires_at);

-- Revoked flag filtering
CREATE INDEX idx_auth_session_revoked
    ON auth_session (revoked);

-- Optional but useful: "my active sessions" queries
-- (e.g., WHERE user_id=? AND revoked=false ORDER BY last_used_at DESC)
CREATE INDEX idx_auth_session_user_active_last_used
    ON auth_session (user_id, revoked, last_used_at DESC);