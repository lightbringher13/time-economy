package com.timeeconomy.auth.domain.auth.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.model.payload.AuthUserRegisteredPayload;
import com.timeeconomy.auth.domain.auth.port.in.RegisterUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SignupSessionStorePort signupSessionRepositoryPort;

    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;

    private final Clock clock;

    @Override
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        if (command == null || command.signupSessionId() == null) {
            return fail(Outcome.NO_SESSION, null);
        }

        if (command.password() == null || command.password().isBlank()) {
            return fail(Outcome.INVALID_INPUT, command.signupSessionId());
        }

        UUID sessionId = command.signupSessionId();
        Instant now = Instant.now(clock);

        SignupSession session = signupSessionRepositoryPort
                .findActiveById(sessionId, now)
                .orElse(null);

        if (session == null) {
            return fail(Outcome.SESSION_EXPIRED_OR_NOT_FOUND, sessionId);
        }

        // ✅ Big-co: only allow registration from PROFILE_READY
        if (session.getState() != SignupSessionState.PROFILE_READY) {
            return fail(Outcome.INVALID_STATE, sessionId);
        }

        // ✅ Source of truth is the session
        String email = session.getEmail();              // already normalized in your domain
        String phoneNumber = session.getPhoneNumber();  // already normalized in your domain

        // duplicates
        if (authUserRepositoryPort.findByEmail(email).isPresent()) {
            return fail(Outcome.EMAIL_ALREADY_USED, sessionId);
        }

        if (authUserRepositoryPort.findByPhoneNumber(phoneNumber).isPresent()) {
            return fail(Outcome.PHONE_ALREADY_USED, sessionId);
        }

        // create user
        String passwordHash = passwordEncoderPort.encode(command.password());

        AuthUser user = new AuthUser(email, passwordHash, phoneNumber);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);

        AuthUser saved = authUserRepositoryPort.save(user);

        // complete session
        session.markCompleted(now);
        signupSessionRepositoryPort.save(session);

        // outbox
        String payloadJson = outboxPayloadSerializerPort.serialize(
                new AuthUserRegisteredPayload(saved.getId(), sessionId)
        );

        OutboxEvent event = OutboxEvent.newPending(
                "auth_user",
                saved.getId().toString(),
                "AuthUserRegistered.v1",
                payloadJson,
                now
        );

        outboxEventRepositoryPort.save(event);

        return new RegisterResult(
                Outcome.REGISTERED,
                true,
                sessionId,
                saved.getId(),
                saved.getEmail()
        );
    }

    private RegisterResult fail(Outcome outcome, UUID sessionId) {
        return new RegisterResult(outcome, false, sessionId, null, null);
    }
}