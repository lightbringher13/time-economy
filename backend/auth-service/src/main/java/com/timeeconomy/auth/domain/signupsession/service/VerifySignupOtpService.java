package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.VerificationChallengeUseCase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifySignupOtpService implements VerifySignupOtpUseCase {

    private final SignupSessionStorePort signupSessionStorePort;
    private final VerificationChallengeUseCase verificationChallengeUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Result verify(Command command) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID sessionId = command.sessionId();

        var sessionOpt = signupSessionStorePort.findActiveById(sessionId, now);
        if (sessionOpt.isEmpty()) {
            return new Result(false, sessionId, false, false, "EXPIRED_OR_NOT_FOUND");
        }

        SignupSession session = sessionOpt.get();

        VerificationPurpose purpose;
        VerificationChannel channel;
        String destination;

        if (command.target() == SignupVerificationTarget.EMAIL) {
            destination = session.getEmail();
            purpose = VerificationPurpose.SIGNUP_EMAIL;
            channel = VerificationChannel.EMAIL;

            if (destination == null || destination.isBlank()) {
                return fail(session);
            }

        } else { // PHONE
            destination = session.getPhoneNumber();
            purpose = VerificationPurpose.SIGNUP_PHONE;
            channel = VerificationChannel.SMS;

            if (destination == null || destination.isBlank()) {
                return fail(session);
            }
        }

        var verify = verificationChallengeUseCase.verifyOtp(
                new VerificationChallengeUseCase.VerifyOtpCommand(
                        VerificationSubjectType.SIGNUP_SESSION,
                        session.getId().toString(),
                        purpose,
                        channel,
                        destination,
                        command.code()
                )
        );

        if (!verify.success()) {
            return fail(session);
        }

        // ✅ update SignupSession
        if (command.target() == SignupVerificationTarget.EMAIL) {
            session.markEmailVerified(now);
        } else {
            session.markPhoneVerified(now);
        }

        signupSessionStorePort.save(session);

        return new Result(true, session.getId(), session.isEmailVerified(), session.isPhoneVerified(), safe(session.getState()));
    }

    private Result fail(SignupSession session) {
        return new Result(
                false,
                session.getId(),
                session.isEmailVerified(),
                session.isPhoneVerified(),
                safe(session.getState())
        );
    }

    private static String safe(Enum<?> e) {
        return e == null ? "NULL" : e.name();
    }
}