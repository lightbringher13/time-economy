package com.timeeconomy.auth_service.domain.auth.port.out;

/**
 * Port for generating & hashing refresh tokens.
 */
public interface RefreshTokenPort {

    String generateFamilyId();                // usually a random UUID
    String generateRefreshToken();            // long random token
    String hashRefreshToken(String rawToken); // SHA-256, etc.
}