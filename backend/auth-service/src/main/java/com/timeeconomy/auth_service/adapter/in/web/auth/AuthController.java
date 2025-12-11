package com.timeeconomy.auth_service.adapter.in.web.auth;

import com.timeeconomy.auth_service.adapter.in.web.auth.dto.request.LoginRequest;
import com.timeeconomy.auth_service.adapter.in.web.auth.dto.request.RegisterRequest;
import com.timeeconomy.auth_service.adapter.in.web.auth.dto.response.AuthResponse;
import com.timeeconomy.auth_service.adapter.in.web.auth.dto.response.LoginResponse;
import com.timeeconomy.auth_service.adapter.in.web.auth.dto.response.RegisterResponse;
import com.timeeconomy.auth_service.adapter.in.web.auth.dto.response.SessionResponseDto;
import com.timeeconomy.auth_service.domain.auth.port.in.ListSessionsUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.LoginUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.LogoutAllUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.LogoutSessionUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.LogoutUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.RefreshUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.RegisterUseCase;
import com.timeeconomy.auth_service.domain.auth.port.in.LoginUseCase.LoginCommand;
import com.timeeconomy.auth_service.domain.auth.port.in.LoginUseCase.LoginResult;
import com.timeeconomy.auth_service.domain.auth.port.in.LogoutUseCase.LogoutCommand;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
        private static final long REFRESH_TTL_SECONDS = 7L * 24 * 60 * 60;

        private final LoginUseCase loginUseCase;
        private final RefreshUseCase refreshUseCase;
        private final LogoutUseCase logoutUseCase;
        private final LogoutAllUseCase logoutAllUseCase;
        private final ListSessionsUseCase listSessionsUseCase;
        private final LogoutSessionUseCase logoutSessionUseCase;
        private final RegisterUseCase registerUseCase;

        public AuthController(LoginUseCase loginUseCase,
                        RefreshUseCase refreshUseCase,
                        LogoutUseCase logoutUseCase,
                        LogoutAllUseCase logoutAllUseCase,
                        ListSessionsUseCase listSessionsUseCase,
                        LogoutSessionUseCase logoutSessionUseCase,
                        RegisterUseCase registerUseCase) {
                this.loginUseCase = loginUseCase;
                this.refreshUseCase = refreshUseCase;
                this.logoutUseCase = logoutUseCase;
                this.logoutAllUseCase = logoutAllUseCase;
                this.listSessionsUseCase = listSessionsUseCase;
                this.logoutSessionUseCase = logoutSessionUseCase;
                this.registerUseCase = registerUseCase;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(
                        @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest) {
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
                                userAgent);

                // ---- Call domain use case ----
                LoginResult result = loginUseCase.login(command);

                // ---- Set refresh token cookie ----
                // (expiry should match what you use in domain; for dev we can pick 30 days)
                ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
                                .httpOnly(true)
                                .secure(true) // 🔹 set true in HTTPS/prod
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
                        HttpServletRequest request) {
                if (refreshToken == null || refreshToken.isBlank()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                var cmd = new RefreshUseCase.RefreshCommand(
                                refreshToken,
                                request.getRemoteAddr(),
                                request.getHeader("User-Agent"),
                                "unknown-device");

                var result = refreshUseCase.refresh(cmd);

                var body = new AuthResponse(result.accessToken());

                // ⭐ Start building the response (cookie optional)
                ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

                // ⭐ Only set cookie if BE returned a new refresh token (normal rotation)
                if (result.refreshToken() != null) {
                        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
                                        .httpOnly(true)
                                        .secure(true)
                                        .path("/")
                                        .maxAge(REFRESH_TTL_SECONDS)
                                        .sameSite("Strict")
                                        .build();

                        builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
                }

                return builder.body(body);
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
                logoutUseCase.logout(new LogoutCommand(refreshToken));

                // delete refresh token cookie
                ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                                .maxAge(0)
                                .httpOnly(true)
                                .secure(true) // keep same as your login cookie
                                .path("/")
                                .sameSite("Strict")
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                                .build();
        }

        // auth-service/src/main/java/com/timeeconomy/auth_service/adapter/in/web/AuthController.java

        @PostMapping("/logout/all")
        public ResponseEntity<Void> logoutAll(
                @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken // just for cookie delete
        ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
                // TODO: 나중에 AuthenticationRequiredException 같은 도메인 예외로 바꿔도 됨
                throw new IllegalStateException("Missing authenticated user id");
        }

        Long authUserId;
        try {
                authUserId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
                throw new IllegalStateException("Invalid authenticated user id: " + userIdHeader);
        }

        logoutAllUseCase.logoutAll(new LogoutAllUseCase.LogoutAllCommand(authUserId));

        // 현재 브라우저의 refreshToken 쿠키도 삭제 (기존 로직 유지)
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
        }

        @GetMapping("/sessions")
        public ResponseEntity<List<SessionResponseDto>> listSessions(
                        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

                ListSessionsUseCase.SessionsResult result = listSessionsUseCase.listSessions(
                                new ListSessionsUseCase.ListSessionsQuery(refreshToken));

                List<SessionResponseDto> body = result.sessions().stream()
                                .map(SessionResponseDto::from)
                                .toList();

                return ResponseEntity.ok(body);
        }

        @DeleteMapping("/sessions/{sessionId}")
        public ResponseEntity<Void> logoutSession(
                        @PathVariable Long sessionId,
                        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
                logoutSessionUseCase.logoutSession(
                                new LogoutSessionUseCase.Command(sessionId, refreshToken));

                return ResponseEntity.noContent().build();
        }

        @PostMapping("/register")
        public ResponseEntity<RegisterResponse> register(
                @CookieValue(name = "signup_session_id", required = false) String signupSessionIdCookie,
                @RequestBody RegisterRequest request
        ) {
        UUID signupSessionId = null;
        if (signupSessionIdCookie != null && !signupSessionIdCookie.isBlank()) {
                try {
                signupSessionId = UUID.fromString(signupSessionIdCookie);
                } catch (IllegalArgumentException ignored) {
                // malformed cookie → treat as no session
                }
        }

        RegisterUseCase.RegisterResult result = registerUseCase.register(
                new RegisterUseCase.RegisterCommand(
                        signupSessionId,          // ⭐ NEW
                        request.email(),
                        request.password(),
                        request.phoneNumber(),
                        request.name(),
                        request.gender(),
                        request.birthDate()
                )
        );

        RegisterResponse body = new RegisterResponse(
                result.userId(),
                result.email()
        );

        // ⭐ Clear signup_session_id cookie after successful registration
        ResponseCookie clearCookie = ResponseCookie.from("signup_session_id", "")
                .path("/")
                .maxAge(0)          // delete
                .httpOnly(true)
                .secure(false)      // TODO: true in production with HTTPS
                .sameSite("Lax")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(body);
        }

}