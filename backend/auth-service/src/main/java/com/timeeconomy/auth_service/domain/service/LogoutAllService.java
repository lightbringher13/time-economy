package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth_service.domain.port.in.LogoutAllUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutAllService implements LogoutAllUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;

    @Override
    @Transactional
    public void logoutAll(LogoutAllCommand command) {
        Long userId = command.authUserId();
        if (userId == null) {
            throw new AuthUserNotFoundException(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        authSessionRepositoryPort.revokeAllByUserId(userId, now);
    }
}