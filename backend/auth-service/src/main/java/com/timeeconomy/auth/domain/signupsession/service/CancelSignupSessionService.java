package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.CancelSignupSessionUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelSignupSessionService implements CancelSignupSessionUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional
    public Result cancel(Command command) {
        final Instant now = Instant.now(clock);

        if (command == null) {
            return new Result(Outcome.INVALID_INPUT, false, null, SignupSessionState.DRAFT);
        }

        final UUID sessionId = command.sessionId();
        if (sessionId == null) {
            return new Result(Outcome.NO_SESSION, false, null, SignupSessionState.EXPIRED);
        }

        // Big-co: “not found” is normal => NO_SESSION (don’t throw)
        final SignupSession session = signupSessionStorePort.findById(sessionId).orElse(null);
        if (session == null) {
            return new Result(Outcome.NO_SESSION, false, sessionId, SignupSessionState.EXPIRED);
        }

        // If expired, treat as NO_SESSION (expected) — don’t throw
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            return new Result(Outcome.NO_SESSION, false, sessionId, SignupSessionState.EXPIRED);
        }

        // idempotent
        if (session.isTerminal()) {
            return new Result(Outcome.ALREADY_TERMINAL, false, session.getId(), session.getState());
        }

        // cancel
        session.cancel(now);
        signupSessionStorePort.save(session);

        // cancel pending challenges (best effort)
        cancelPendingOtp(now, sessionId, VerificationPurpose.SIGNUP_EMAIL, VerificationChannel.EMAIL);
        cancelPendingOtp(now, sessionId, VerificationPurpose.SIGNUP_PHONE, VerificationChannel.SMS);

        return new Result(Outcome.CANCELED, true, session.getId(), session.getState());
    }

    private void cancelPendingOtp(
            Instant now,
            UUID sessionId,
            VerificationPurpose purpose,
            VerificationChannel channel
    ) {
        verificationChallengeRepositoryPort.findActivePending(
                VerificationSubjectType.SIGNUP_SESSION,
                sessionId.toString(),
                purpose,
                channel
        ).ifPresent(ch -> {
            if (ch.getStatus() == VerificationStatus.PENDING) {
                ch.cancel(now);
                verificationChallengeRepositoryPort.save(ch);
            }
        });
    }
}