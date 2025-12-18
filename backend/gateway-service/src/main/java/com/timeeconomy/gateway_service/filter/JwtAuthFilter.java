package com.timeeconomy.gateway_service.filter;

import com.timeeconomy.gateway_service.exception.UnauthorizedException;
import com.timeeconomy.gateway_service.security.JwtValidator;
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
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

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
            "/api/auth/public/verification/otp",
            "/api/auth/public/verification/otp/verify",
            "/api/auth/signup/verify-otp",
            "/api/auth/signup/send-otp"
    );

    // ✅ 필요하면 prefix 기반으로 완전 공개할 path들
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            // 예: /api/auth/password/reset?token=... 처럼 쿼리 사용하는 경우
            "/api/auth/password/reset"
    );

    private boolean isPublicPath(String path) {
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1) CORS preflight (OPTIONS)은 항상 통과
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // 2) 완전 공개 엔드포인트는 JWT 검사 없이 통과
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 3) 그 외는 모두 JWT 필요
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

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

            ServerWebExchange mutated = exchange.mutate()
                    .request(builder -> builder.header("X-User-Id", userId))
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