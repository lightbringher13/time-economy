package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.auth.port.in.LogoutUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth.domain.exception.SessionAlreadyRevokedException;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final RefreshTokenPort refreshTokenPort;

    private final Clock clock;

    @Override
    public void logout(LogoutCommand command) {

        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new MissingRefreshTokenException();
        }

        String hash = refreshTokenPort.hashRefreshToken(command.refreshToken());

        var sessionOpt = authSessionRepositoryPort.findByTokenHash(hash);

        if (sessionOpt.isEmpty()) {
            throw new AuthSessionNotFoundException();
        }

        var session = sessionOpt.get();

        if (session.isRevoked()) {
            throw new SessionAlreadyRevokedException();
        }
        Instant now = Instant.now(clock);
        session.revoke(now);
        authSessionRepositoryPort.save(session);
    }
}
