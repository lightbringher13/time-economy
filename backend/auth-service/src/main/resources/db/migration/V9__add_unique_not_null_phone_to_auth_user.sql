-- V0xx__add_unique_not_null_phone_to_auth_user.sql

ALTER TABLE auth_user
    ALTER COLUMN phone_number SET NOT NULL;

ALTER TABLE auth_user
    ADD CONSTRAINT uq_auth_user_phone UNIQUE (phone_number);