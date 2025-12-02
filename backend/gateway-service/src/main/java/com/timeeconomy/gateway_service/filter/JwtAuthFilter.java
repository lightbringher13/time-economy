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

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1) health check pass-through
        if ("/api/health".equals(path)) {
            return chain.filter(exchange);
        }

        // 2) /api/auth/** pass-through
        if (path.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        // 3) allow OPTIONS
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // 4) Authorization validation
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
                throw new UnauthorizedException("Access token subject (user id) is missing.");
            }

            // Add downstream header
            ServerWebExchange mutated = exchange.mutate()
                    .request(builder ->
                            builder.header("X-User-Id", userId)
                    )
                    .build();

            return chain.filter(mutated);

        } catch (JwtException e) {
            throw new UnauthorizedException("Access token is invalid or expired.");
        } catch (Exception e) {
            throw new UnauthorizedException("Unexpected gateway authentication error.");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}