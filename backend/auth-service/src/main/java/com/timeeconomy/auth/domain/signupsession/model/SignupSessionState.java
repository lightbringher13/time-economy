package com.timeeconomy.auth.domain.signupsession.model;

public enum SignupSessionState {
    DRAFT,                 // session created, user typed email/phone but hasn't started OTP yet

    EMAIL_OTP_SENT,        // email OTP issued (cooldown applies)
    EMAIL_VERIFIED,        // email verified ✅

    PHONE_OTP_SENT,        // SMS OTP issued
    PHONE_VERIFIED,        // phone verified ✅

    PROFILE_PENDING,       // both verified, profile not submitted yet (name, etc.)
    COMPLETED,             // register finished (auth_user created)

    CANCELED,              // user canceled flow
    EXPIRED                // TTL expired
}