package com.timeeconomy.auth.domain.auth.port.out;

/**
 * Port for issuing access tokens (JWT or any other type).
 */
public interface JwtTokenPort {

    /**
     * Generate an access token for the given user and family.
     */
    String generateAccessToken(Long userId);
}