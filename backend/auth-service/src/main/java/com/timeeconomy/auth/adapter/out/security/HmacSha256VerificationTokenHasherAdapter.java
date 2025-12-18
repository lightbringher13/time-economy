package com.timeeconomy.auth.adapter.out.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class HmacSha256VerificationTokenHasherAdapter implements VerificationTokenHasherPort {

    private final String secret;

    public HmacSha256VerificationTokenHasherAdapter(
            @Value("${app.verification.hmac-secret}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.verification.hmac-secret is missing");
        }
        this.secret = secret;
    }

    @Override
    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken is blank");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}