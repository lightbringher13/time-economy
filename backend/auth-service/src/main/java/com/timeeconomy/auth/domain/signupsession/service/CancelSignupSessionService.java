// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/CancelSignupSessionService.java
package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionNotActiveException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
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
    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort; // ✅ optional but good
    private final Clock clock;

    @Override
    @Transactional
    public Result cancel(Command command) {
        Instant now = Instant.now(clock);
        UUID sessionId = command.sessionId();

        SignupSession session = signupSessionStorePort.findById(sessionId)
                .orElseThrow(() -> new SignupSessionNotFoundException(sessionId));

        // expire materialization
        if (session.expireIfNeeded(now)) {
            signupSessionStorePort.save(session);
            throw new SignupSessionNotActiveException("Signup session expired");
        }

        // idempotent: already terminal -> return current state
        if (session.isTerminal()) {
            return new Result(session.getId(), session.getState());
        }

        // mark canceled
        session.cancel(now);
        signupSessionStorePort.save(session);

        // ✅ cancel any pending signup OTP challenges (email + phone)
        cancelPendingOtp(now, sessionId, VerificationPurpose.SIGNUP_EMAIL, VerificationChannel.EMAIL);
        cancelPendingOtp(now, sessionId, VerificationPurpose.SIGNUP_PHONE, VerificationChannel.SMS);

        return new Result(session.getId(), session.getState());
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