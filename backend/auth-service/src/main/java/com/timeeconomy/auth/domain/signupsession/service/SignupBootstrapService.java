package com.timeeconomy.auth.domain.signupsession.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.SignupBootstrapUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupBootstrapService implements SignupBootstrapUseCase {

    private static final Duration SIGNUP_SESSION_TTL = Duration.ofHours(24);

    private final SignupSessionStorePort signupSessionStorePort;
    private final Clock clock;

    @Override
    public Result bootstrap(Command command) {
        Instant now = Instant.now(clock);
        UUID existingId = command.existingSessionId();

        SignupSession session = null;
        boolean reused = false;

        // 1) try existing session id from cookie
        if (existingId != null) {
            session = signupSessionStorePort.findActiveById(existingId, now).orElse(null);

            // "active" might still include COMPLETED/CANCELED depending on store impl
            if (session != null && !isBootstrappable(session)) {
                session = null;
            }

            reused = (session != null);
        }

        // 2) if no reusable session → create new empty session
        if (session == null) {
            Instant expiresAt = now.plus(SIGNUP_SESSION_TTL);

            // ✅ Prefer: create empty session in DRAFT state
            session = SignupSession.createNew(now, expiresAt);
            // If you update createNew() to not take email: SignupSession.createNew(now, expiresAt);

            session = signupSessionStorePort.save(session);
        }

        // 3) map to result
        return new Result(
                session.getId(),
                reused, // ✅ meaningful: true if we reused cookie session, false if created new
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

    private boolean isBootstrappable(SignupSession session) {
        SignupSessionState s = session.getState();
        if (s == null) return true;

        // ✅ terminal states should not be reused
        return s != SignupSessionState.COMPLETED
                && s != SignupSessionState.CANCELED
                && s != SignupSessionState.EXPIRED;
    }
}