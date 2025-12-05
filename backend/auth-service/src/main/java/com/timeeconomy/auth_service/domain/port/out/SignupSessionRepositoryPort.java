package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.SignupSession;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SignupSessionRepositoryPort {

    /**
     * Save or update a signup session.
     */
    SignupSession save(SignupSession session);

    /**
     * Find session by id (regardless of state/expiry).
     */
    Optional<SignupSession> findById(UUID id);

    // ⭐ NEW: Find latest non-expired, non-completed signup session by email
    Optional<SignupSession> findLatestActiveByEmail(String email, LocalDateTime now);

    /**
     * Find session by id that is not expired and not completed.
     * Convenient for use cases like:
     *  - continue signup
     *  - verify email for an active session
     */
    default Optional<SignupSession> findActiveById(UUID id, LocalDateTime now) {
        return findById(id)
                .filter(session -> !session.isExpired(now))
                .filter(session -> !session.isCompleted());
    }
}