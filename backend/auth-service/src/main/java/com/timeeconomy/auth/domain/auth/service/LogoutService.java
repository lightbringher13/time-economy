package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.auth.port.in.LogoutUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth.domain.exception.SessionAlreadyRevokedException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final RefreshTokenPort refreshTokenPort;

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

        session.revoke(LocalDateTime.now());
        authSessionRepositoryPort.save(session);
    }
}
