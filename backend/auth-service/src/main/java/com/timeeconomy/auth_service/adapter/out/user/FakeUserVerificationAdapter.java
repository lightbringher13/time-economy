package com.timeeconomy.auth_service.adapter.out.user;

import com.timeeconomy.auth_service.domain.port.out.UserVerificationPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Temporary fake adapter for UserVerificationPort.
 *
 * In real life this will call the user-service (HTTP/Feign/gRPC).
 * For now we just hard-code one demo user so the login flow works.
 */
@Component
public class FakeUserVerificationAdapter implements UserVerificationPort {

    // Demo user – you can change these values as you like
    private static final Long DEMO_USER_ID = 1L;
    private static final String DEMO_EMAIL = "test@test.com";
    private static final String DEMO_PASSWORD = "test";

    @Override
    public Optional<Long> verifyCredentials(String email, String rawPassword) {
        // Very naive: just compare plain strings.
        // This is ONLY for dev while we don't have real user-service.
        if (DEMO_EMAIL.equals(email) && DEMO_PASSWORD.equals(rawPassword)) {
            return Optional.of(DEMO_USER_ID);
        }
        return Optional.empty();
    }
}