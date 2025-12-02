package com.timeeconomy.gateway_service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timeeconomy.gateway_service.exception.UnauthorizedException;
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

        // 1) 우리 custom 401
        if (ex instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
            message = ex.getMessage();
        }
        // 2) WebFlux에서 던지는 404, 405, 400 등 그대로 사용
        else if (ex instanceof ResponseStatusException rse) {
            // rse.getStatusCode()는 HttpStatusCode 타입이니까 value()로 int 꺼내서 HttpStatus로 변환
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = (rse.getReason() != null) ? rse.getReason() : rse.getMessage();
        }
        // 3) 그 외는 전부 500
        else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal gateway error";
        }

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "service", "gateway-service",
                "code", status.name(),
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