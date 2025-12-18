package com.timeeconomy.auth_service.adapter.out.redis.signupsession;

import lombok.Builder;

@Builder
public record SignupSessionSnapshot(
        int schemaVersion,
        String id,

        String email,
        String emailVerified,
        String phoneNumber,
        String phoneVerified,

        String name,
        String gender,
        String birthDate,

        String state,

        String createdAt,
        String updatedAt,
        String expiresAt
) {}