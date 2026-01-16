package com.timeeconomy.auth.domain.signupsession.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSession;

public interface SignupSessionStorePort {

    /**
     * Save or update a signup session.
     */
    SignupSession save(SignupSession session);

    /**
     * Find session by id (regardless of state/expiry).
     */
    Optional<SignupSession> findById(UUID id);

    SignupSession createNew(Instant now);

    /**
     * Find latest non-expired, non-completed signup session by email.
     */
    Optional<SignupSession> findLatestActiveByEmail(String email, Instant now);

    /**
     * Find session by id that is not expired and not completed.
     */
    default Optional<SignupSession> findActiveById(UUID id, Instant now) {
        return findById(id)
            .filter(s -> !s.isTerminal())
            .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(now));
    }
}