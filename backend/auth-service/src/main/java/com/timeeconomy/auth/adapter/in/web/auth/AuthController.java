package com.timeeconomy.auth.adapter.in.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.auth.dto.request.LoginRequest;
import com.timeeconomy.auth.adapter.in.web.auth.dto.request.RegisterRequest;
import com.timeeconomy.auth.adapter.in.web.auth.dto.response.AuthResponse;
import com.timeeconomy.auth.adapter.in.web.auth.dto.response.LoginResponse;
import com.timeeconomy.auth.adapter.in.web.auth.dto.response.RegisterResponse;
import com.timeeconomy.auth.adapter.in.web.auth.dto.response.SessionResponseDto;

// ✅ Your existing global error DTO (adjust import if needed)
import com.timeeconomy.auth.adapter.in.web.common.dto.response.ApiErrorResponse;

import com.timeeconomy.auth.domain.auth.port.in.ListSessionsUseCase;
import com.timeeconomy.auth.domain.auth.port.in.LoginUseCase;
import com.timeeconomy.auth.domain.auth.port.in.LogoutAllUseCase;
import com.timeeconomy.auth.domain.auth.port.in.LogoutSessionUseCase;
import com.timeeconomy.auth.domain.auth.port.in.LogoutUseCase;
import com.timeeconomy.auth.domain.auth.port.in.RefreshUseCase;
import com.timeeconomy.auth.domain.auth.port.in.RegisterUseCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long REFRESH_TTL_SECONDS = 7L * 24 * 60 * 60;

    private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";

    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final LogoutAllUseCase logoutAllUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final LogoutSessionUseCase logoutSessionUseCase;
    private final RegisterUseCase registerUseCase;

    // ----------------------------------------------------------------
    // Auth
    // ----------------------------------------------------------------

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = Optional.ofNullable(request.ipAddress())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> clientIp(httpRequest));

        String userAgent = Optional.ofNullable(request.userAgent())
                .filter(s -> !s.isBlank())
                .orElse(httpRequest.getHeader("User-Agent"));

        String deviceInfo = Optional.ofNullable(request.deviceInfo())
                .filter(s -> !s.isBlank())
                .orElse("unknown-device");

        var result = loginUseCase.login(new LoginUseCase.LoginCommand(
                request.email(),
                request.password(),
                deviceInfo,
                ip,
                userAgent
        ));

        ResponseCookie refreshCookie = buildRefreshCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var result = refreshUseCase.refresh(new RefreshUseCase.RefreshCommand(
                refreshToken,
                clientIp(request),
                request.getHeader("User-Agent"),
                "unknown-device"
        ));

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        // Only set cookie if rotated token returned
        if (result.refreshToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString());
        }

        return builder.body(new AuthResponse(result.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        logoutUseCase.logout(new LogoutUseCase.LogoutCommand(refreshToken));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }

    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalStateException("Missing authenticated user id");
        }

        long authUserId;
        try {
            authUserId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid authenticated user id: " + userIdHeader);
        }

        logoutAllUseCase.logoutAll(new LogoutAllUseCase.LogoutAllCommand(authUserId));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponseDto>> listSessions(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        var result = listSessionsUseCase.listSessions(new ListSessionsUseCase.ListSessionsQuery(refreshToken));

        List<SessionResponseDto> body = result.sessions().stream()
                .map(SessionResponseDto::from)
                .toList();

        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> logoutSession(
            @PathVariable Long sessionId,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        logoutSessionUseCase.logoutSession(new LogoutSessionUseCase.Command(sessionId, refreshToken));
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Register (UPDATED: success DTO vs error ApiErrorResponse)
    // ----------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String signupSessionIdCookie,
            @Valid @RequestBody RegisterRequest request
    ) {
        UUID signupSessionId = parseUuidOrNull(signupSessionIdCookie);

        RegisterUseCase.RegisterResult r = registerUseCase.register(
                new RegisterUseCase.RegisterCommand(signupSessionId, request.password())
        );

        return toRegisterHttp(r);
    }

    private ResponseEntity<?> toRegisterHttp(RegisterUseCase.RegisterResult r) {
        // ✅ success → 201 + clear signup cookie
        if (r.outcome() == RegisterUseCase.Outcome.REGISTERED) {
            RegisterResponse body = new RegisterResponse(
                    r.outcome(),
                    true,
                    r.userId(),
                    r.email()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, deleteSignupCookie().toString())
                    .body(body);
        }

        // ❌ expected failures → 4xx + ApiErrorResponse(code,message)
        HttpStatus status = switch (r.outcome()) {
            case NO_SESSION, SESSION_EXPIRED_OR_NOT_FOUND -> HttpStatus.UNAUTHORIZED; // 401
            case INVALID_STATE, EMAIL_ALREADY_USED, PHONE_ALREADY_USED -> HttpStatus.CONFLICT; // 409
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST; // 400
            default -> HttpStatus.BAD_REQUEST;
        };

        String message = switch (r.outcome()) {
            case NO_SESSION, SESSION_EXPIRED_OR_NOT_FOUND ->
                    "Your signup session is missing or expired. Please start again.";
            case INVALID_STATE ->
                    "Signup is not ready to register yet. Please complete previous steps.";
            case EMAIL_ALREADY_USED ->
                    "That email is already in use. Please change it and verify again.";
            case PHONE_ALREADY_USED ->
                    "That phone number is already in use. Please change it and verify again.";
            case INVALID_INPUT ->
                    "Invalid request.";
            default ->
                    "Registration failed.";
        };

        ApiErrorResponse err = new ApiErrorResponse(r.outcome().name(), message);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);

        // Optional UX: clear signup cookie when session is missing/expired
        if (r.outcome() == RegisterUseCase.Outcome.NO_SESSION
                || r.outcome() == RegisterUseCase.Outcome.SESSION_EXPIRED_OR_NOT_FOUND) {
            builder.header(HttpHeaders.SET_COOKIE, deleteSignupCookie().toString());
        }

        return builder.body(err);
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest http) {
        // proxy-aware
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = http.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return http.getRemoteAddr();
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)   // TODO: env-controlled for local (http)
                .path("/")
                .maxAge(REFRESH_TTL_SECONDS)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)   // keep consistent with build cookie
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie deleteSignupCookie() {
        return deleteCookie(SIGNUP_SESSION_COOKIE);
    }
}