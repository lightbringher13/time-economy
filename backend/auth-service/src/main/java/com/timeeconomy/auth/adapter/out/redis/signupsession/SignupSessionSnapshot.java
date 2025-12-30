package com.timeeconomy.auth.adapter.out.redis.signupsession;

import lombok.Builder;

@Builder
public record SignupSessionSnapshot(
        int schemaVersion,
        String id,

        String email,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified,

        String name,
        String gender,
        Integer birthDateEpochDays, // LocalDate.toEpochDay()

        String state,

        Long createdAtEpochMillis,   // Instant.toEpochMilli()
        Long updatedAtEpochMillis,
        Long expiresAtEpochMillis
) {}