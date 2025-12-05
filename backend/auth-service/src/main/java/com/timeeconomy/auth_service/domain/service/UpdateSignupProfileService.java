package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.model.SignupSession;
import com.timeeconomy.auth_service.domain.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth_service.domain.port.out.SignupSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSignupProfileService implements UpdateSignupProfileUseCase {

    private final SignupSessionRepositoryPort signupSessionRepositoryPort;

    @Override
    @Transactional
    public void updateProfile(Command command) {
        UUID sessionId = command.sessionId();
        LocalDateTime now = LocalDateTime.now();

        SignupSession session = signupSessionRepositoryPort
                .findActiveById(sessionId, now)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // domain method on SignupSession you should have:
        session.updateProfile(
                command.name(),
                command.phoneNumber(),
                command.gender(),
                command.birthDate(),
                now
        );

        signupSessionRepositoryPort.save(session);
    }
}