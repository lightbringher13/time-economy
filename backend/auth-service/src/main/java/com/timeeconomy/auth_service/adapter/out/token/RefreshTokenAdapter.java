package com.timeeconomy.auth_service.adapter.out.token;

import org.springframework.stereotype.Component;

import com.timeeconomy.auth_service.domain.auth.port.out.RefreshTokenPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateFamilyId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[64]; // 512-bit token
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }
}