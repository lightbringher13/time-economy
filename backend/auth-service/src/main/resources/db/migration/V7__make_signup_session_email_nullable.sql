-- VXX__make_signup_session_email_nullable.sql
ALTER TABLE signup_sessions
    ALTER COLUMN email DROP NOT NULL;