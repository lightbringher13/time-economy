// gateway-service/src/main/java/com/timeeconomy/gateway_service/adapter/in/web/HealthController.java
package com.timeeconomy.gateway.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "service", "gateway-service",
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                )
        );
    }
}