-- Final user_profile table (PostgreSQL)

CREATE TABLE user_profile (
    id           BIGINT       NOT NULL,          -- reuse auth-service userId
    email        VARCHAR(255) NOT NULL,
    name         VARCHAR(100) NULL,
    phone_number VARCHAR(30)  NULL,
    birth_date   DATE         NULL,
    gender       VARCHAR(10)  NULL,

    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_profile PRIMARY KEY (id),
    CONSTRAINT uq_user_profile_email UNIQUE (email)
);

-- Optional but common: keep updated_at fresh automatically
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_profile_updated_at
BEFORE UPDATE ON user_profile
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();