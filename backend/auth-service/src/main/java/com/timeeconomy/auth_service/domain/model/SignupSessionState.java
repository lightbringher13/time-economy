package com.timeeconomy.auth_service.domain.model;

public enum SignupSessionState {
    EMAIL_PENDING,      // email entered, code maybe sent
    EMAIL_VERIFIED,     // email verified, profile data may still be empty
    PROFILE_FILLED,     // profile fields (name/phone/gender/birthDate) filled
    COMPLETED,          // signup completed -> auth_user & user_profile created
    EXPIRED             // session expired / invalidated
}