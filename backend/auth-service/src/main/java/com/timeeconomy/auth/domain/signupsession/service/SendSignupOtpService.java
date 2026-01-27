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

        // 1) fast-fail duplicates (via SignupSession port)
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

        // 2) load-or-create
        LoadOrCreate loaded = loadOrCreateSession(command.sessionId(), now);
        SignupSession session = loaded.session();
        boolean sessionCreated = loaded.created();

        // 3) state guard for phone flow
        if (target == SignupVerificationTarget.PHONE && !session.isEmailVerified()) {
            return fromSession(Outcome.INVALID_STATE, false, session, sessionCreated, null, 0, null, null);
        }

        // 4) idempotency: already verified & destination matches
        if (target == SignupVerificationTarget.EMAIL
                && session.isEmailVerified()
                && destination.equals(normalizeEmail(session.getEmail()))) {
            return fromSession(Outcome.ALREADY_VERIFIED, false, session, sessionCreated, null, 0, null, null);
        }

        if (target == SignupVerificationTarget.PHONE
                && session.isPhoneVerified()
                && destination.equals(normalizePhone(session.getPhoneNumber()))) {
            return fromSession(Outcome.ALREADY_VERIFIED, false, session, sessionCreated, null, 0, null, null);
        }

        // 5) issuer params
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

        // 6) issue OTP (Verification decides TTL/maxAttempts/policy)
        SignupOtpIssuerPort.IssueResult issued = otpIssuerPort.issue(new SignupOtpIssuerPort.IssueCommand(
                session.getId().toString(),
                purpose,
                channel,
                destination,
                command.requestIp(),
                command.userAgent()
        ));

        // Safety: if adapter returns null, treat as delivery failure
        if (issued == null) {
            return fromSession(Outcome.DELIVERY_FAILED, false, session, sessionCreated, null, 0, null, null);
        }

        int ttlMinutes = toTtlMinutes(now, issued.expiresAt());

        // ✅ Do not mutate SignupSession if delivery didn't happen
        switch (issued.outcome()) {
            case SENT -> {
                // continue
            }

            case THROTTLED -> {
                return fromSession(
                        Outcome.THROTTLED,
                        false,
                        session,
                        sessionCreated,
                        issued.challengeId(),
                        ttlMinutes,
                        issued.maskedDestination(),
                        issued.retryAfterSeconds() // ✅ pass through
                );
            }

            case INVALID_DESTINATION, INVALID_INPUT -> {
                return invalidDestination(command.sessionId());
            }

            case DELIVERY_FAILED -> {
                return fromSession(
                        Outcome.DELIVERY_FAILED,
                        false,
                        session,
                        sessionCreated,
                        issued.challengeId(),
                        ttlMinutes,
                        issued.maskedDestination(),
                        null
                );
            }
        }

        // 7) apply domain command AFTER issuance (source-of-truth expiresAt)
        try {
            if (target == SignupVerificationTarget.EMAIL) {
                session.requestEmailOtp(destination, issued.expiresAt(), now);
            } else {
                session.requestPhoneOtp(destination, issued.expiresAt(), now);
            }
        } catch (IllegalArgumentException e) {
            return fromSession(Outcome.INVALID_DESTINATION, false, session, sessionCreated, null, 0, null, null);
        } catch (IllegalStateException e) {
            return fromSession(Outcome.INVALID_STATE, false, session, sessionCreated, null, 0, null, null);
        }

        signupSessionStorePort.save(session);

        return fromSession(
                Outcome.SENT,
                true,
                session,
                sessionCreated,
                issued.challengeId(),
                ttlMinutes,
                issued.maskedDestination(),
                issued.retryAfterSeconds()
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
                // stale cookie => new session (UX)
                .orElseGet(() -> new LoadOrCreate(signupSessionStorePort.createNew(now), true));
    }

    private int toTtlMinutes(Instant now, Instant expiresAt) {
        if (expiresAt == null) return 0;
        long mins = Duration.between(now, expiresAt).toMinutes();
        return (int) Math.max(1, mins);
    }

    private Result invalidInput(UUID sessionId) {
        return new Result(
                Outcome.INVALID_INPUT,
                false,
                sessionId,
                false,
                null, 0, null,
                null,                 // ✅ retryAfterSeconds
                null, null,
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
                null,                 // ✅ retryAfterSeconds
                null, null,
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
                null,                 // ✅ retryAfterSeconds
                null, null,
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
            String maskedDestination,
            Integer retryAfterSeconds
    ) {
        return new Result(
                outcome,
                sent,
                session.getId(),
                sessionCreated,
                challengeId,
                ttlMinutes,
                maskedDestination,
                retryAfterSeconds,                 // ✅ correct slot
                session.getEmailOtpExpiresAt(),
                session.getPhoneOtpExpiresAt(),
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