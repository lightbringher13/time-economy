package com.timeeconomy.auth.domain.auth.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.in.RegisterUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.EmailNotVerifiedException;
import com.timeeconomy.auth.domain.exception.PhoneNotVerifiedException;
import com.timeeconomy.auth.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SignupSessionStorePort signupSessionRepositoryPort;

    // ✅ OUTBOX
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;

    // ✅ OUTBOX PAYLOAD SERIALIZER (port)
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;

    @Override
    @Transactional
    public RegisterResult register(RegisterCommand command) {
        if (command.signupSessionId() == null) {
            throw new SignupSessionNotFoundException("Missing signup session");
        }

        LocalDateTime now = LocalDateTime.now();

        SignupSession session = signupSessionRepositoryPort
                .findActiveById(command.signupSessionId(), now)
                .orElseThrow(() -> new SignupSessionNotFoundException("Signup session not found or expired"));

        String email = normalizeEmail(command.email());

        if (session.getEmail() == null || !session.getEmail().equalsIgnoreCase(email)) {
            throw new SignupSessionNotFoundException("Email mismatch between signup session and request");
        }

        if (!session.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified to register");
        }

        if (!session.isPhoneVerified()) {
            throw new PhoneNotVerifiedException("Phone number must be verified to register");
        }

        authUserRepositoryPort.findByEmail(email).ifPresent(existing -> {
            session.setEmailVerified(false);
            signupSessionRepositoryPort.save(session);
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        authUserRepositoryPort.findByPhoneNumber(command.phoneNumber()).ifPresent(existing -> {
            session.setPhoneVerified(false);
            signupSessionRepositoryPort.save(session);
            throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
        });

        String passwordHash = passwordEncoderPort.encode(command.password());

        AuthUser user = new AuthUser(email, passwordHash, command.phoneNumber());
        user.setEmailVerified(true);
        user.setPhoneVerified(true);

        AuthUser saved = authUserRepositoryPort.save(user);

        session.markCompleted(now);
        signupSessionRepositoryPort.save(session);

        // ✅ OUTBOX EVENT APPEND (for CDC / Debezium)
        String payloadJson = outboxPayloadSerializerPort.serialize(new AuthUserRegisteredPayload(
                saved.getId(),
                saved.getEmail(),
                saved.getPhoneNumber(),
                command.name(),
                command.gender(),
                command.birthDate(),
                command.signupSessionId(),
                now
        ));

        OutboxEvent event = OutboxEvent.newPending(
                "auth_user",               // aggregateType
                saved.getId().toString(),   // aggregateId
                "AuthUserRegistered.v1",    // eventType
                payloadJson,
                now
        );

        outboxEventRepositoryPort.save(event);

        return new RegisterResult(saved.getId(), saved.getEmail());
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }

    // ✅ payload schema (versioned by eventType: ...v1)
    record AuthUserRegisteredPayload(
            Long userId,
            String email,
            String phoneNumber,
            String name,
            String gender,
            LocalDate birthDate,
            UUID signupSessionId,
            LocalDateTime occurredAt
    ) {}
}