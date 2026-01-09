package com.timeeconomy.auth.domain.signupsession.service;

import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.port.in.EditSignupPhoneUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EditSignupPhoneService implements EditSignupPhoneUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final AuthUserRepositoryPort authUserRepositoryPort;

    // optional but recommended
    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort;

    private final Clock clock;

    @Override
    @Transactional
    public Result editPhone(Command command) {
        Instant now = Instant.now(clock);

        String newPhone = normalizePhone(command.newPhoneNumber());
        if (newPhone == null || newPhone.isBlank()) {
            throw new IllegalArgumentException("newPhoneNumber is required");
        }

        SignupSession session = signupSessionStorePort
                .findActiveById(command.sessionId(), now)
                .orElseThrow(() -> new SignupSessionNotFoundException(command.sessionId()));

        // ✅ fast-fail: prevent already-used phone
        authUserRepositoryPort.findByPhoneNumber(newPhone).ifPresent(existing -> {
            throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
        });

        // ✅ domain action (moves back to EMAIL_VERIFIED and invalidates downstream profile)
        session.editPhone(newPhone, now);

        // ✅ optional: cancel pending phone OTP for this session
        verificationChallengeRepositoryPort.findActivePending(
                VerificationSubjectType.SIGNUP_SESSION,
                session.getId().toString(),
                VerificationPurpose.SIGNUP_PHONE,
                VerificationChannel.SMS
        ).ifPresent(ch -> {
            if (ch.getStatus() == VerificationStatus.PENDING) {
                ch.cancel(now);
                verificationChallengeRepositoryPort.save(ch);
            }
        });

        signupSessionStorePort.save(session);

        return map(session);
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.trim(); // TODO E.164 later
    }

    private static Result map(SignupSession s) {
        return new Result(
                s.getId(),
                s.getEmail(),
                s.isEmailVerified(),
                s.getPhoneNumber(),
                s.isPhoneVerified(),
                s.getName(),
                s.getGender(),
                s.getBirthDate(),
                s.getState()
        );
    }
}