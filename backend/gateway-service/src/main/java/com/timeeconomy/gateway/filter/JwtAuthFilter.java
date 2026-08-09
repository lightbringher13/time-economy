package com.timeeconomy.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.timeeconomy.gateway.exception.UnauthorizedException;
import com.timeeconomy.gateway.security.JwtValidator;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final JwtValidator jwtValidator;

    // ✅ 명시적으로 "완전 공개" 엔드포인트만 정리
    private static final List<String> PUBLIC_EXACT_PATHS = List.of(
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/password/forgot",
            "/api/auth/password/reset",
            "/api/auth/phone/request-code",
            "/api/auth/phone/verify",
            "/api/auth/email/verify",
            "/api/auth/email/send-code",
            "/api/auth/signup/bootstrap",
            "/api/auth/signup/profile",
            "/api/auth/signup/verify-otp",
            "/api/auth/signup/send-otp",
            "/api/auth/signup/resend-otp",
            "/api/auth/signup/edit-email",
            "/api/auth/signup/edit-phone",
            "/api/auth/signup/cancel",
            "/api/auth/signup/status"
    );

    private boolean isPublicPath(String path) {
        return PUBLIC_EXACT_PATHS.contains(path);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.remove(USER_ID_HEADER)))
                .build();

        String path = sanitizedExchange.getRequest().getURI().getPath();
        HttpMethod method = sanitizedExchange.getRequest().getMethod();

        // 1) CORS preflight (OPTIONS)은 항상 통과
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(sanitizedExchange);
        }

        // 2) 완전 공개 엔드포인트는 JWT 검사 없이 통과
        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        // 3) 그 외는 모두 JWT 필요
        String authHeader = sanitizedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isBlank()) {
            throw new UnauthorizedException("Authorization header is required.");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header must start with 'Bearer '.");
        }

        String token = authHeader.substring(7);

        try {
            Jws<Claims> claims = jwtValidator.validate(token);
            String userId = claims.getPayload().getSubject();

            if (userId == null || userId.isBlank()) {
                throw new UnauthorizedException("INVALID_SUBJECT", "Access token subject missing.");
            }

            ServerWebExchange mutated = sanitizedExchange.mutate()
                    .request(builder -> builder.headers(headers -> headers.set(USER_ID_HEADER, userId)))
                    .build();

            return chain.filter(mutated);

        } catch (JwtException e) {
            String code = e.getMessage();

            if ("TOKEN_EXPIRED".equals(code)) {
                throw new UnauthorizedException("ACCESS_TOKEN_EXPIRED", "Access token expired.");
            }

            throw new UnauthorizedException("ACCESS_TOKEN_INVALID", "Access token invalid.");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
