package com.timeeconomy.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.timeeconomy.gateway.exception.UnauthorizedException;
import com.timeeconomy.gateway.security.JwtValidator;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

class JwtAuthFilterTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String JWT_ISSUER = "timeeconomy-auth";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final JwtAuthFilter filter = new JwtAuthFilter(
            new JwtValidator(JWT_SECRET, JWT_ISSUER)
    );

    @Test
    void publicRequestRemovesUntrustedUserIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/health")
                .header("x-user-id", "attacker")
                .build();

        ServerWebExchange forwarded = forward(request);

        assertNull(forwarded.getRequest().getHeaders().getFirst(USER_ID_HEADER));
    }

    @Test
    void optionsRequestRemovesUntrustedUserIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.options("/api/users/me")
                .header(USER_ID_HEADER, "attacker")
                .build();

        ServerWebExchange forwarded = forward(request);

        assertNull(forwarded.getRequest().getHeaders().getFirst(USER_ID_HEADER));
    }

    @Test
    void authenticatedRequestReplacesSpoofedUserIdWithJwtSubject() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("42"))
                .header(USER_ID_HEADER, "attacker")
                .build();

        ServerWebExchange forwarded = forward(request);

        assertEquals(
                List.of("42"),
                forwarded.getRequest().getHeaders().get(USER_ID_HEADER)
        );
    }

    @Test
    void removedPublicVerificationEndpointsRequireAuthentication() {
        for (String path : List.of(
                "/api/auth/public/verification/otp",
                "/api/auth/public/verification/otp/verify"
        )) {
            assertUnauthorized(MockServerHttpRequest.post(path).build());
        }
    }

    @Test
    void passwordResetQueryUsesExactPublicPath() {
        MockServerHttpRequest request = MockServerHttpRequest.post(
                "/api/auth/password/reset?token=reset-token"
        ).header(USER_ID_HEADER, "attacker").build();

        ServerWebExchange forwarded = forward(request);

        assertNull(forwarded.getRequest().getHeaders().getFirst(USER_ID_HEADER));
    }

    @Test
    void passwordResetPathSuffixIsNotPublic() {
        MockServerHttpRequest request = MockServerHttpRequest.post(
                "/api/auth/password/reset-anything"
        ).build();

        assertUnauthorized(request);
    }

    private ServerWebExchange forward(MockServerHttpRequest request) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };

        filter.filter(MockServerWebExchange.from(request), chain).block();

        return Objects.requireNonNull(forwarded.get());
    }

    private void assertUnauthorized(MockServerHttpRequest request) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        assertThrows(
                UnauthorizedException.class,
                () -> filter.filter(MockServerWebExchange.from(request), chain).block()
        );
        assertFalse(chainCalled.get());
    }

    private String accessToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .issuer(JWT_ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
