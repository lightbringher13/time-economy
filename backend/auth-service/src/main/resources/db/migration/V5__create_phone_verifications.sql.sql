-- Vxx__create_phone_verifications.sql

CREATE TABLE phone_verifications (
    id            BIGSERIAL PRIMARY KEY,
    phone_number  VARCHAR(20)  NOT NULL,
    country_code  VARCHAR(10)  NOT NULL DEFAULT '+82',
    code          VARCHAR(10)  NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    verified_at   TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 최근 시도 빠르게 조회
CREATE INDEX idx_phone_verifications_phone_created_at
    ON phone_verifications (phone_number, created_at DESC);

-- 같은 번호에 동일 코드 중복 방지
CREATE UNIQUE INDEX uq_phone_verifications_phone_code
    ON phone_verifications (phone_number, code);