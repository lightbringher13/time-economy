-- Password reset tokens table
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,

    email       VARCHAR(255) NOT NULL,

    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP    NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 빠른 조회용 인덱스
CREATE INDEX idx_password_reset_tokens_token_hash
    ON password_reset_tokens (token_hash);

CREATE INDEX idx_password_reset_tokens_email_created_at
    ON password_reset_tokens (email, created_at DESC);