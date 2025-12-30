CREATE TABLE auth_user (
    id                      BIGSERIAL PRIMARY KEY,

    -- Link to user-service (can be NULL if auth-user created before user-service sync)
    user_id                  BIGINT UNIQUE,

    -- Login identifier
    email                    VARCHAR(255) NOT NULL UNIQUE,

    -- Hashed password (BCrypt, Argon2, etc.)
    password_hash            VARCHAR(255) NOT NULL,

    -- ACTIVE / LOCKED / DELETED / PENDING ...
    status                   VARCHAR(20) NOT NULL,

    failed_login_attempts    INT NOT NULL DEFAULT 0,

    -- ✅ Instant-friendly timestamps
    locked_at                TIMESTAMPTZ NULL,
    last_login_at            TIMESTAMPTZ NULL,

    -- Contact + verification flags
    phone_number             VARCHAR(30) NOT NULL UNIQUE,
    email_verified           BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified           BOOLEAN NOT NULL DEFAULT FALSE,

    -- ✅ Instant-friendly timestamps
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Useful indexes (optional)
-- CREATE INDEX idx_auth_user_status ON auth_user(status);
-- CREATE INDEX idx_auth_user_user_id ON auth_user(user_id);