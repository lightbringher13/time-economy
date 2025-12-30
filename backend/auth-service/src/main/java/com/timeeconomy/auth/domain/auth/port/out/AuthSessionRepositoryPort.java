package com.timeeconomy.auth.domain.auth.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.timeeconomy.auth.domain.auth.model.AuthSession;

public interface AuthSessionRepositoryPort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(Long id);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    Optional<AuthSession> findLatestActiveByFamily(String familyId, Instant now);

    /**
     * All non-revoked sessions for a user (for /me/sessions etc).
     */
    List<AuthSession> findActiveByUserId(Long userId);

    Optional<AuthSession> findByTokenHashForUpdate(String tokenHash);

    /**
     * Mark a single session revoked.
     */
    void revokeById(Long id, Instant now);

    /**
     * Revoke all active sessions in the same device family.
     * Used when a refresh token reuse attack is detected.
     */
    void revokeFamily(String familyId, Instant now);

    /**
     * Revoke all sessions of a user (logout-all).
     */
    void revokeAllByUserId(Long userId, Instant now);
}