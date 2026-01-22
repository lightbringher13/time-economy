package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.model.SignupVerificationTarget;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupIdentityUniquenessPort;
import com.timeeconomy.auth.domain.signupsession.port.out.integration.SignupOtpIssuerPort;
import com.timeeconomy.auth.domain.signupsession.port.out.persistence.SignupSessionStorePort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendSignupOtpService implements SendSignupOtpUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    // ✅ SignupSession-owned dependencies
    private final SignupSessionStorePort signupSessionStorePort;
    private final SignupIdentityUniquenessPort uniquenessPort;
    private final SignupOtpIssuerPort otpIssuerPort;

    private final Clock clock;

    @Override
    @Transactional
    public Result send(Command command) {
        if (command == null || command.target() == null) {
            return invalidInput(command == null ? null : command.sessionId());
        }

        final Instant now = Instant.now(clock);
        final SignupVerificationTarget target = command.target();
        final String destination = normalizeDestination(target, command.destination());

        if (destination == null) {
            return invalidDestination(command.sessionId());
        }

        // 1) fast-fail duplicates (no exceptions, via SignupSession port)
        if (target == SignupVerificationTarget.EMAIL) {
            var u = uniquenessPort.checkEmail(destination);
            if (u.outcome() == SignupIdentityUniquenessPort.Outcome.EMAIL_ALREADY_USED) {
                return simpleOutcome(Outcome.EMAIL_ALREADY_USED, command.sessionId());
            }
            if (u.outcome() == SignupIdentityUniquenessPort.Outcome.INVALID_INPUT) {
                return invalidDestination(command.sessionId());
            }
        } else {
            var u = uniquenessPort.checkPhone(destination);
            if (u.outcome() == SignupIdentityUniquenessPort.Outcome.PHONE_ALREADY_USED) {
                return simpleOutcome(Outcome.PHONE_ALREADY_USED, command.sessionId());
            }
            if (u.outcome() == SignupIdentityUniquenessPort.Outcome.INVALID_INPUT) {
                return invalidDestination(command.sessionId());
            }
        }

        // 2) load-or-create (tolerant: stale cookie => create new)
        LoadOrCreate loaded = loadOrCreateSession(command.sessionId(), now);
        SignupSession session = loaded.session();
        boolean sessionCreated = loaded.created();

        // 3) state guard for phone flow (don’t throw)
        if (target == SignupVerificationTarget.PHONE && !session.isEmailVerified()) {
            return fromSession(Outcome.INVALID_STATE, false, session, sessionCreated, null, 0, null);
        }

        // 4) idempotency: already verified & destination matches
        if (target == SignupVerificationTarget.EMAIL
                && session.isEmailVerified()
                && destination.equals(normalizeEmail(session.getEmail()))) {
            return fromSession(Outcome.ALREADY_VERIFIED, false, session, sessionCreated, null, 0, null);
        }

        if (target == SignupVerificationTarget.PHONE
                && session.isPhoneVerified()
                && destination.equals(normalizePhone(session.getPhoneNumber()))) {
            return fromSession(Outcome.ALREADY_VERIFIED, false, session, sessionCreated, null, 0, null);
        }

        // 5) resolve issuer params (SignupSession-owned enums)
        final SignupOtpIssuerPort.Purpose purpose =
                (target == SignupVerificationTarget.EMAIL)
                        ? SignupOtpIssuerPort.Purpose.SIGNUP_EMAIL
                        : SignupOtpIssuerPort.Purpose.SIGNUP_PHONE;

        final SignupOtpIssuerPort.Channel channel =
                (target == SignupVerificationTarget.EMAIL)
                        ? SignupOtpIssuerPort.Channel.EMAIL
                        : SignupOtpIssuerPort.Channel.SMS;

        log.info("[SignupSendOtp] sessionId={} created={} target={} purpose={} channel={} destMasked={} ip={} uaPresent={}",
                session.getId(),
                sessionCreated,
                target,
                purpose,
                channel,
                mask(channel, destination),
                safe(command.requestIp()),
                command.userAgent() != null && !command.userAgent().isBlank()
        );

        // 6) issue OTP first (via SignupSession port)
        var issued = otpIssuerPort.issue(new SignupOtpIssuerPort.IssueCommand(
                session.getId().toString(),
                purpose,
                channel,
                destination,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                command.requestIp(),
                command.userAgent()
        ));

        // ✅ If throttled / not sent, do NOT mutate session facts/state
        if (!issued.sent()) {
            return fromSession(
                    Outcome.THROTTLED,
                    false,
                    session,
                    sessionCreated,
                    issued.challengeId(),
                    issued.ttlMinutes(),
                    issued.maskedDestination()
            );
        }

        // 7) apply domain command AFTER OTP issuance
        try {
            if (target == SignupVerificationTarget.EMAIL) {
                session.requestEmailOtp(destination, now);
            } else {
                session.requestPhoneOtp(destination, now);
            }
        } catch (IllegalArgumentException e) {
            return fromSession(Outcome.INVALID_DESTINATION, false, session, sessionCreated, null, 0, null);
        } catch (IllegalStateException e) {
            return fromSession(Outcome.INVALID_STATE, false, session, sessionCreated, null, 0, null);
        }

        signupSessionStorePort.save(session);

        return fromSession(
                Outcome.SENT,
                true,
                session,
                sessionCreated,
                issued.challengeId(),
                issued.ttlMinutes(),
                issued.maskedDestination()
        );
    }

    // ---------------------------
    // helpers
    // ---------------------------

    private record LoadOrCreate(SignupSession session, boolean created) {}

    private LoadOrCreate loadOrCreateSession(UUID sessionId, Instant now) {
        if (sessionId == null) {
            SignupSession created = signupSessionStorePort.createNew(now);
            return new LoadOrCreate(created, true);
        }

        return signupSessionStorePort.findActiveById(sessionId, now)
                .map(s -> new LoadOrCreate(s, false))
                // stale cookie => create new (nice UX)
                .orElseGet(() -> new LoadOrCreate(signupSessionStorePort.createNew(now), true));
    }

    private Result invalidInput(UUID sessionId) {
        return new Result(
                Outcome.INVALID_INPUT,
                false,
                sessionId,
                false,
                null, 0, null,
                false, false, false, false,
                SignupSessionState.DRAFT
        );
    }

    private Result invalidDestination(UUID sessionId) {
        return new Result(
                Outcome.INVALID_DESTINATION,
                false,
                sessionId,
                false,
                null, 0, null,
                false, false, false, false,
                SignupSessionState.DRAFT
        );
    }

    private Result simpleOutcome(Outcome outcome, UUID sessionId) {
        return new Result(
                outcome,
                false,
                sessionId,
                false,
                null, 0, null,
                false, false, false, false,
                SignupSessionState.DRAFT
        );
    }

    private Result fromSession(
            Outcome outcome,
            boolean sent,
            SignupSession session,
            boolean sessionCreated,
            String challengeId,
            int ttlMinutes,
            String maskedDestination
    ) {
        return new Result(
                outcome,
                sent,
                session.getId(),
                sessionCreated,
                challengeId,
                ttlMinutes,
                maskedDestination,
                session.isEmailVerified(),
                session.isPhoneVerified(),
                session.isEmailOtpPending(),
                session.isPhoneOtpPending(),
                session.getState()
        );
    }

    private String normalizeDestination(SignupVerificationTarget target, String raw) {
        if (raw == null || raw.isBlank()) return null;
        return (target == SignupVerificationTarget.EMAIL) ? normalizeEmail(raw) : normalizePhone(raw);
    }

    private String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    private String normalizePhone(String raw) {
        return raw == null ? null : raw.trim();
    }

    private String mask(SignupOtpIssuerPort.Channel channel, String destination) {
        if (destination == null) return "***";
        if (channel == SignupOtpIssuerPort.Channel.EMAIL) {
            int at = destination.indexOf('@');
            if (at <= 1) return "***" + destination.substring(Math.max(0, at));
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (destination.length() <= 4) return "***";
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }

    private String safe(String v) {
        return (v == null) ? "-" : v;
    }
}