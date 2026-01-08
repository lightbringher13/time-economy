package com.timeeconomy.auth.domain.signupsession.service;

import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.port.in.EditSignupEmailUseCase;
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
public class EditSignupEmailService implements EditSignupEmailUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final AuthUserRepositoryPort authUserRepositoryPort;

    // optional but recommended
    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort;

    private final Clock clock;

    @Override
    @Transactional
    public Result editEmail(Command command) {
        Instant now = Instant.now(clock);

        String newEmail = normalizeEmail(command.newEmail());
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("newEmail is required");
        }

        SignupSession session = signupSessionStorePort
                .findActiveById(command.sessionId(), now)
                .orElseThrow(() -> new SignupSessionNotFoundException(command.sessionId()));

        // ✅ fast-fail: prevent already-used email
        authUserRepositoryPort.findByEmail(newEmail).ifPresent(existing -> {
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // ✅ domain action (resets downstream proof/state as you implemented)
        session.editEmail(newEmail, now);

        // ✅ optional: cancel any pending OTP challenges for this session
        cancelPendingOtp(VerificationPurpose.SIGNUP_EMAIL, VerificationChannel.EMAIL, session.getId().toString(), now);
        cancelPendingOtp(VerificationPurpose.SIGNUP_PHONE, VerificationChannel.SMS, session.getId().toString(), now);

        signupSessionStorePort.save(session);

        return map(session);
    }

    private void cancelPendingOtp(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String subjectId,
            Instant now
    ) {
        verificationChallengeRepositoryPort.findActivePending(
                VerificationSubjectType.SIGNUP_SESSION,
                subjectId,
                purpose,
                channel
        ).ifPresent(ch -> {
            if (ch.getStatus() == VerificationStatus.PENDING) {
                ch.cancel(now);
                verificationChallengeRepositoryPort.save(ch);
            }
        });
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
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