package com.timeeconomy.auth_service.domain.port.out;

import com.timeeconomy.auth_service.domain.model.AuthSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthSessionRepositoryPort {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(Long id);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    /**
     * All non-revoked sessions for a user (for /me/sessions etc).
     */
    List<AuthSession> findActiveByUserId(Long userId);

    Optional<AuthSession> findByTokenHashForUpdate(String tokenHash);

    /**
     * Mark a single session revoked.
     */
    void revokeById(Long id, LocalDateTime now);

    /**
     * Revoke all active sessions in the same device family.
     * Used when a refresh token reuse attack is detected.
     */
    void revokeFamily(String familyId, LocalDateTime now);

    /**
     * Revoke all sessions of a user (logout-all).
     */
    void revokeAllByUserId(Long userId, LocalDateTime now);
}