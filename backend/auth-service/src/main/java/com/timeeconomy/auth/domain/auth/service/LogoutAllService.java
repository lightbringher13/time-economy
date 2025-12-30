package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.port.in.LogoutAllUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutAllService implements LogoutAllUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;

    private final Clock clock;

    @Override
    @Transactional
    public void logoutAll(LogoutAllCommand command) {
        Long userId = command.authUserId();
        if (userId == null) {
            throw new AuthUserNotFoundException(userId);
        }

        Instant now = Instant.now(clock);
        authSessionRepositoryPort.revokeAllByUserId(userId, now);
    }
}