package com.timeeconomy.auth_service.adapter.out.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth_service.domain.auth.port.out.JwtTokenPort;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-exp-seconds}")
    private long accessTokenExpSeconds;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey key;
    private MacAlgorithm alg;

    @PostConstruct
    public void init() {
        // HS256 algorithm
        this.alg = Jwts.SIG.HS256;

        // JJWT 0.12+ expects binary key for HMAC
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(Long userId) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, alg)
                .compact();
    }
}