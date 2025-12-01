CREATE TABLE user_profile (
    id           BIGINT       NOT NULL,          -- auth-service의 userId 재사용
    email        VARCHAR(255) NOT NULL,
    name         VARCHAR(100),
    phone_number VARCHAR(30),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_profile PRIMARY KEY (id),
    CONSTRAINT uq_user_profile_email UNIQUE (email)
);