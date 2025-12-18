package com.timeeconomy.auth_service.domain.verification.model;

/**
 * Keep purposes explicit. Add more as features grow.
 */
public enum VerificationPurpose {
    SIGNUP_EMAIL,
    SIGNUP_PHONE,
    CHANGE_EMAIL_NEW,
    CHANGE_EMAIL_2FA_OLD_EMAIL,
    CHANGE_EMAIL_2FA_PHONE,
    PASSWORD_RESET
}