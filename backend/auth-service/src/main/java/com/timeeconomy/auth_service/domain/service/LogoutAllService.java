package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.model.AuthSession;
import com.timeeconomy.auth_service.domain.port.out.RefreshTokenPort;
import com.timeeconomy.auth_service.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth_service.domain.port.in.LogoutAllUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthSessionRepositoryPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutAllService implements LogoutAllUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public void logoutAll(LogoutAllCommand command) {
        String raw = command.refreshToken();
        if (raw == null || raw.isBlank()) {
            // 도메인 예외 재사용 가능
            throw new MissingRefreshTokenException();
        }

        String hash = refreshTokenPort.hashRefreshToken(raw);

        // 하나의 세션을 먼저 찾고, 거기서 userId 를 얻는다
        AuthSession session = authSessionRepositoryPort.findByTokenHash(hash)
                .orElseThrow(AuthSessionNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        authSessionRepositoryPort.revokeAllByUserId(session.getUserId(), now);
    }
}