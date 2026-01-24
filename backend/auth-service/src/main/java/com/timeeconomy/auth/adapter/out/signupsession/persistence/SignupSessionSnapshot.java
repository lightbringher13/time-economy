package com.timeeconomy.auth.adapter.out.signupsession.persistence;

import lombok.Builder;

@Builder
public record SignupSessionSnapshot(
        int schemaVersion,
        String id,

        String email,
        boolean emailVerified,
        boolean emailOtpPending,

        // ✅ NEW: OTP expiry for email verification
        Long emailOtpExpiresAtEpochMillis,

        String phoneNumber,
        boolean phoneVerified,
        boolean phoneOtpPending,

        // ✅ NEW: OTP expiry for phone verification
        Long phoneOtpExpiresAtEpochMillis,

        String name,
        String gender,
        Integer birthDateEpochDays,

        String state,

        Long createdAtEpochMillis,
        Long updatedAtEpochMillis,
        Long expiresAtEpochMillis
) {}