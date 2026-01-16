package com.timeeconomy.auth.domain.signupsession.model;

public enum SignupSessionState {
    DRAFT,          // email exists but not verified, and no email OTP pending
    EMAIL_OTP_SENT, // email OTP pending (waiting for code)
    EMAIL_VERIFIED, // email verified, phone not verified yet

    PHONE_OTP_SENT, // phone OTP pending (waiting for code)

    PROFILE_PENDING,// both verified, profile incomplete
    PROFILE_READY,  // both verified, profile complete (ready to register)

    COMPLETED,      // account created
    CANCELED,       // canceled by user
    EXPIRED         // expired by TTL
}