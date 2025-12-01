CREATE TABLE auth_user (
    id                  BIGSERIAL       PRIMARY KEY,
    
    -- user-service와 연결하기 위한 외부 유저 ID (options: 나중에 null 허용해도 됨)
    user_id             BIGINT         UNIQUE,
    
    -- 로그인에 사용하는 계정 식별자
    email               VARCHAR(255)   NOT NULL UNIQUE,
    
    -- BCrypt 등 해시된 비밀번호
    password_hash       VARCHAR(255)   NOT NULL,
    
    -- 계정 상태: ACTIVE, LOCKED, DELETED, PENDING 등
    status              VARCHAR(20)    NOT NULL,
    
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_at           TIMESTAMP,
    
    last_login_at       TIMESTAMP,
    
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 상태 + 이메일 조회 많이 쓸 거면 인덱스 (email은 이미 UNIQUE라 OK)
-- CREATE INDEX idx_auth_user_status ON auth_user(status);