package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.LoginRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.LoginResponse;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase.LoginCommand;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        // ---- Enrich / fallback for device info, ip, user-agent ----
        String ip = Optional.ofNullable(request.ipAddress())
                .filter(s -> !s.isBlank())
                .orElseGet(httpRequest::getRemoteAddr);

        String userAgent = Optional.ofNullable(request.userAgent())
                .filter(s -> !s.isBlank())
                .orElse(httpRequest.getHeader("User-Agent"));

        String deviceInfo = Optional.ofNullable(request.deviceInfo())
                .filter(s -> !s.isBlank())
                .orElse("unknown-device");

        // ---- Build command for use case ----
        LoginCommand command = new LoginCommand(
                request.email(),
                request.password(),
                deviceInfo,
                ip,
                userAgent
        );

        // ---- Call domain use case ----
        LoginResult result = loginUseCase.login(command);

        // ---- Set refresh token cookie ----
        // (expiry should match what you use in domain; for dev we can pick 30 days)
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)  // 🔹 set true in HTTPS/prod
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();

        // ---- Return access token in JSON ----
        LoginResponse responseBody = new LoginResponse(result.accessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseBody);
    }
}