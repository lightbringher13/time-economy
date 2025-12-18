package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.port.in.SignupBootstrapUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

// domain.service
@Service
@RequiredArgsConstructor
public class SignupBootstrapService implements SignupBootstrapUseCase {

    private static final Duration SIGNUP_SESSION_TTL = Duration.ofHours(24);

    private final SignupSessionStorePort signupSessionStorePort;

    @Override
    public Result bootstrap(Command command) {
        LocalDateTime now = LocalDateTime.now();
        UUID existingId = command.existingSessionId();

        SignupSession session = null;

        // 1) try existing session id from cookie
        if (existingId != null) {
            session = signupSessionStorePort
                    .findActiveById(existingId, now)
                    .orElse(null);
        }

        // 2) if no active session → create new empty session
        if (session == null) {
            LocalDateTime expiresAt = now.plus(SIGNUP_SESSION_TTL);
            session = SignupSession.createNew(
                    null,           // email 아직 모름
                    now,
                    expiresAt
            );
            session = signupSessionStorePort.save(session);
        }

        // 3) map to result
        return new Result(
                session.getId(),
                true,                         // 항상 active 하나는 존재
                session.getEmail(),
                session.isEmailVerified(),
                session.getPhoneNumber(),
                session.isPhoneVerified(),
                session.getName(),
                session.getGender(),
                session.getBirthDate() != null ? session.getBirthDate().toString() : null,
                session.getState() != null ? session.getState().name() : null
        );
    }
}