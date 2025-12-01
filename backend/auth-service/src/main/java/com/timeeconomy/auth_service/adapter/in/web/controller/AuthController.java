package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.AuthResponse;
import com.timeeconomy.auth_service.adapter.in.web.dto.LoginRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.LoginResponse;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase.LoginCommand;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase.LoginResult;
import com.timeeconomy.auth_service.domain.port.in.LogoutUseCase.LogoutCommand;
import com.timeeconomy.auth_service.domain.port.in.RefreshUseCase;
import com.timeeconomy.auth_service.domain.port.in.LogoutUseCase;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long REFRESH_TTL_SECONDS = 7L * 24 * 60 * 60;

    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(LoginUseCase loginUseCase, RefreshUseCase refreshUseCase, LogoutUseCase logoutUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
        this.logoutUseCase = logoutUseCase;
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
                .maxAge(REFRESH_TTL_SECONDS)
                .sameSite("Strict")
                .build();

        // ---- Return access token in JSON ----
        LoginResponse responseBody = new LoginResponse(result.accessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var cmd = new RefreshUseCase.RefreshCommand(
                refreshToken,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                "unknown-device" // deviceInfo: optional for now
        );

        var result = refreshUseCase.refresh(cmd);

        // 🔁 set new refresh token cookie
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)   // false only in local HTTP if needed
                .path("/")
                .maxAge(REFRESH_TTL_SECONDS)
                .sameSite("Strict")
                .build();

        var body = new AuthResponse(result.accessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
                @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false)
                String refreshToken
        ) {
        logoutUseCase.logout(new LogoutCommand(refreshToken));

        // delete refresh token cookie
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)        // keep same as your login cookie
                .path("/")
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
        }     
}