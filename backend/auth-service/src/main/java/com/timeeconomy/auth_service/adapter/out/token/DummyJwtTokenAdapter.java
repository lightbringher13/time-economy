package com.timeeconomy.auth_service.adapter.out.token;

import com.timeeconomy.auth_service.domain.port.out.JwtTokenPort;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

/**
 * Simple dummy implementation for now.
 * Later we can replace this with a real JWT implementation using jjwt.
 */
@Component
public class DummyJwtTokenAdapter implements JwtTokenPort {

    @Override
    public String generateAccessToken(Long userId, String familyId) {
        // Very simple: encode "userId:familyId:random"
        String payload = userId + ":" + familyId + ":" + UUID.randomUUID();
        return "ACCESS_" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
    }
}