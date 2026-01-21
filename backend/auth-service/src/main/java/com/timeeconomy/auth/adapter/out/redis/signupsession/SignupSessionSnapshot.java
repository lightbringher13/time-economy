package com.timeeconomy.auth.adapter.out.redis.signupsession;

import lombok.Builder;

@Builder
public record SignupSessionSnapshot(
        int schemaVersion,
        String id,

        String email,
        boolean emailVerified,
        boolean emailOtpPending,     // ✅ NEW (big-co fact)

        String phoneNumber,
        boolean phoneVerified,
        boolean phoneOtpPending,     // ✅ NEW (big-co fact)

        String name,
        String gender,
        Integer birthDateEpochDays,  // LocalDate.toEpochDay()

        String state,

        Long createdAtEpochMillis,   // Instant.toEpochMilli()
        Long updatedAtEpochMillis,
        Long expiresAtEpochMillis
) {}