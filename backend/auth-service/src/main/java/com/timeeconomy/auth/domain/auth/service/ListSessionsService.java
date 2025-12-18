package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.auth.model.AuthSession;
import com.timeeconomy.auth.domain.auth.port.in.ListSessionsUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.exception.InvalidRefreshTokenException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSessionsService implements ListSessionsUseCase {

    private final RefreshTokenPort refreshTokenPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;

    @Override
    public SessionsResult listSessions(ListSessionsQuery query) {
        String raw = query.rawRefreshToken();
        if (raw == null || raw.isBlank()) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }

        // 1) hash → 현재 세션 찾기
        String hash = refreshTokenPort.hashRefreshToken(raw);

        AuthSession currentSession = authSessionRepositoryPort.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        Long userId = currentSession.getUserId();

        // 2) 유저의 active 세션들 조회 (revoked=false 기준)
        List<AuthSession> sessions =
                authSessionRepositoryPort.findActiveByUserId(userId);

        // 3) DTO 변환 + 현재 세션 표시
        List<SessionInfo> infos = sessions.stream()
                .map(s -> new SessionInfo(
                        s.getId(),
                        s.getFamilyId(),
                        s.getDeviceInfo(),
                        s.getIpAddress(),
                        s.getUserAgent(),
                        s.getCreatedAt(),
                        s.getLastUsedAt(),
                        s.getExpiresAt(),
                        s.isRevoked(),
                        s.getId().equals(currentSession.getId())
                ))
                .toList();

        return new SessionsResult(infos);
    }
}