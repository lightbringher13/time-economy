package com.timeeconomy.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeeconomy.gateway.exception.UnauthorizedException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
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

        HttpStatus status;
        String message;
        String code;  // ⭐ 반드시 있어야 함

        // 1) 우리 custom 401
        if (ex instanceof UnauthorizedException ue) {
            status = HttpStatus.UNAUTHORIZED;
            message = ue.getMessage();
            code = ue.getCode();   // ⭐ FE가 분석하는 핵심 값
        }

        // 2) WebFlux 내부에서 던지는 404, 405, 400 등
        else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

            message = (rse.getReason() != null) ? rse.getReason() : rse.getMessage();
            code = status.name();  // ex) BAD_REQUEST, NOT_FOUND
        }

        // 3) 나머지는 500
        else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal gateway error";
            code = "INTERNAL_ERROR";
        }

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "service", "gateway-service",
                "code", code,
                "message", message,
                "status", status.value(),
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = """
                {"success": false, "service": "gateway-service", "message": "Gateway error"}
            """.getBytes();
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}