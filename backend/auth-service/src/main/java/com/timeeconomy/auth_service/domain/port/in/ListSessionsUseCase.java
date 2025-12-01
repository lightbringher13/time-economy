package com.timeeconomy.auth_service.domain.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface ListSessionsUseCase {

    /**
     * raw refresh token (쿠키에서 읽은 값)을 받아
     * 해당 유저의 세션 목록을 반환한다.
     */
    SessionsResult listSessions(ListSessionsQuery query);

    record ListSessionsQuery(String rawRefreshToken) { }

    record SessionInfo(
            Long sessionId,
            String familyId,
            String deviceInfo,
            String ipAddress,
            String userAgent,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt,
            LocalDateTime expiresAt,
            boolean revoked,
            boolean current // 현재 요청이 사용한 세션인지 표시
    ) { }

    record SessionsResult(List<SessionInfo> sessions) { }
}