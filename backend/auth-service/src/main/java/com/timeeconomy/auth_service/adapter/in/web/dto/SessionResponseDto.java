package com.timeeconomy.auth_service.adapter.in.web.dto;

import com.timeeconomy.auth_service.domain.port.in.ListSessionsUseCase;

import java.time.LocalDateTime;

public record SessionResponseDto(
        Long sessionId,
        String familyId,
        String deviceInfo,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime expiresAt,
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