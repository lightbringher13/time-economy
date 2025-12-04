-- Add email_verified column to users table
ALTER TABLE auth_user
ADD COLUMN phone_number VARCHAR(30),
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;