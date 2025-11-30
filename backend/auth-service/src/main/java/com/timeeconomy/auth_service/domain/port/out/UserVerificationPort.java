package com.timeeconomy.auth_service.domain.port.out;

import java.util.Optional;

/**
 * Port to ask "is this email/password valid?"
 * In real life this will call user-service.
 */
public interface UserVerificationPort {

    /**
     * @return Optional userId if credentials are valid, or empty if invalid.
     */
    Optional<Long> verifyCredentials(String email, String rawPassword);
}