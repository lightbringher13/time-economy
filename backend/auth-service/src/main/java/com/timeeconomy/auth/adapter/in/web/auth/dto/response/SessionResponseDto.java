package com.timeeconomy.auth.adapter.in.web.auth.dto.response;

import java.time.Instant;

import com.timeeconomy.auth.domain.auth.port.in.ListSessionsUseCase;

public record SessionResponseDto(
        Long sessionId,
        String familyId,
        String deviceInfo,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean revoked,
        boolean current
) {
    public static SessionResponseDto from(ListSessionsUseCase.SessionInfo info) {
        return new SessionResponseDto(
                info.sessionId(),
                info.familyId(),
                info.deviceInfo(),
                info.ipAddress(),
                info.userAgent(),
                info.createdAt(),
                info.lastUsedAt(),
                info.expiresAt(),
                info.revoked(),
                info.current()
        );
    }
}