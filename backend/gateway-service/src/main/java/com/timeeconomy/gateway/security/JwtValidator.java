package com.timeeconomy.gateway.security;

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
            throw new JwtException("TOKEN_EXPIRED", e);

        } catch (UnsupportedJwtException e) {
            throw new JwtException("UNSUPPORTED", e);

        } catch (MalformedJwtException e) {
            throw new JwtException("MALFORMED", e);

        } catch (SecurityException | SignatureException e) {
            throw new JwtException("INVALID_SIGNATURE", e);

        } catch (IllegalArgumentException e) {
            throw new JwtException("INVALID_TOKEN", e);

        } catch (Exception e) {
            throw new JwtException("JWT_ERROR", e);
        }
    }
}