package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.model.AuthSession;
import com.timeeconomy.auth.domain.auth.port.in.LogoutSessionUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.exception.InvalidRefreshTokenException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutSessionService implements LogoutSessionUseCase {

    private final RefreshTokenPort refreshTokenPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;

    @Override
    @Transactional
    public void logoutSession(Command command) {
        String raw = command.refreshToken();
        if (raw == null || raw.isBlank()) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }

        // Step 1: identify requester
        String requesterHash = refreshTokenPort.hashRefreshToken(raw);

        AuthSession requester = authSessionRepositoryPort
                .findByTokenHash(requesterHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        Long userId = requester.getUserId();

        // Step 2: find target session
        AuthSession targetSession = authSessionRepositoryPort
                .findById(command.sessionId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Session not found"));

        // Step 3: ensure both belong to same user
        if (!targetSession.getUserId().equals(userId)) {
            throw new InvalidRefreshTokenException("Forbidden");
        }

        // Step 4: revoke target session
        targetSession.revoke(LocalDateTime.now());
        authSessionRepositoryPort.save(targetSession);
    }
}