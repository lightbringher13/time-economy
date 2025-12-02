package com.timeeconomy.gateway_service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeeconomy.gateway_service.exception.UnauthorizedException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
@Order(-1)
public class GatewayErrorHandler implements WebExceptionHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "service", "gateway-service",
                "code", status.name(),
                "message", ex.getMessage(),
                "status", status.value(),
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"success\": false, \"message\": \"Gateway error\"}").getBytes();
        }

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}