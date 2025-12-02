package com.timeeconomy.gateway_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidator {

    private final SecretKey key;
    private final String issuer;

    public JwtValidator(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public Jws<Claims> validate(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);

        } catch (ExpiredJwtException e) {
            // Token has expired
            throw new JwtException("Token expired", e);

        } catch (UnsupportedJwtException e) {
            throw new JwtException("Unsupported JWT", e);

        } catch (MalformedJwtException e) {
            throw new JwtException("Malformed JWT", e);

        } catch (SecurityException | SignatureException e) {
            throw new JwtException("Invalid signature", e);

        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid token", e);

        } catch (Exception e) {
            throw new JwtException("JWT validation error", e);
        }
    }
}