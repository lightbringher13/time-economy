package com.timeeconomy.auth.adapter.in.web.signupsession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.SendSignupOtpRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.UpdateSignupProfileRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.VerifySignupOtpRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.CancelSignupSessionResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.SendSignupOtpResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.SignupStatusResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.VerifySignupOtpResponse;

import com.timeeconomy.auth.domain.signupsession.port.in.CancelSignupSessionUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;

@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class SignupSessionController {

    private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";
    private static final Duration SIGNUP_SESSION_TTL = Duration.ofDays(1);

    private final UpdateSignupProfileUseCase updateSignupProfileUseCase;
    private final VerifySignupOtpUseCase verifySignupOtpUseCase;
    private final SendSignupOtpUseCase sendSignupOtpUseCase;
    private final GetSignupSessionStatusUseCase getSignupSessionStatusUseCase;
    private final CancelSignupSessionUseCase cancelSignupSessionUseCase;

    // -------------------------
    // helpers
    // -------------------------

    private UUID parseCookieUuidOrNull(String sessionIdValue) {
        if (sessionIdValue == null || sessionIdValue.isBlank()) return null;
        try {
            return UUID.fromString(sessionIdValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ResponseCookie buildSignupCookie(UUID sessionId) {
        return ResponseCookie.from(SIGNUP_SESSION_COOKIE, sessionId.toString())
                .httpOnly(true)
                .path("/")
                .secure(true)
                // In local dev, Strict can also be annoying; Lax is often used.
                .sameSite("Strict") // TODO: env-controlled (prod may use Strict)
                .maxAge(SIGNUP_SESSION_TTL)
                .build();
    }

    private ResponseCookie deleteSignupCookie() {
        return ResponseCookie.from(SIGNUP_SESSION_COOKIE, "")
                .httpOnly(true)
                .path("/")
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    private String clientIp(HttpServletRequest http) {
        // Big-co: prefer X-Forwarded-For / X-Real-IP when behind proxy
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = http.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return http.getRemoteAddr();
    }

    // -------------------------
    // Status (lazy-open friendly)
    // -------------------------
    @GetMapping("/status")
    public ResponseEntity<SignupStatusResponse> status(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        var result = getSignupSessionStatusUseCase.getStatus(
                new GetSignupSessionStatusUseCase.Query(sessionId)
        );

        return ResponseEntity.ok(new SignupStatusResponse(
                result.exists(),
                result.email(),
                result.emailVerified(),
                result.emailOtpPending(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.phoneOtpPending(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        ));
    }

    // -------------------------
    // Update profile (requires session)
    // -------------------------
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody UpdateSignupProfileRequest body
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);
        if (sessionId == null) {
            return ResponseEntity.status(401).build();
        }

        updateSignupProfileUseCase.updateProfile(
                new UpdateSignupProfileUseCase.Command(
                        sessionId,
                        body.name(),
                        body.gender(),
                        body.birthDate()
                )
        );

        return ResponseEntity.noContent().build();
    }

    // -------------------------
    // Verify OTP (cookie-only, no request.sessionId)
    // -------------------------
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifySignupOtpResponse> verifyOtp(
            @Valid @RequestBody VerifySignupOtpRequest req,
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String cookieSessionId
    ) {
        UUID sessionId = parseCookieUuidOrNull(cookieSessionId);

        var result = verifySignupOtpUseCase.verify(
                new VerifySignupOtpUseCase.Command(
                        sessionId,        // may be null -> usecase returns NO_SESSION outcome
                        req.target(),
                        req.code()
                )
        );

        return ResponseEntity.ok(new VerifySignupOtpResponse(
                result.outcome(),
                result.success(),
                result.sessionId(),
                result.emailVerified(),
                result.phoneVerified(),
                result.emailOtpPending(),
                result.phoneOtpPending(),
                result.state()
        ));
    }

    // -------------------------
    // Send OTP (lazy-open + destination required)
    // -------------------------
    @PostMapping("/send-otp")
    public ResponseEntity<SendSignupOtpResponse> sendOtp(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody SendSignupOtpRequest body,
            HttpServletRequest http
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");

        var result = sendSignupOtpUseCase.send(new SendSignupOtpUseCase.Command(
                sessionId,          // may be null -> lazy-open in usecase
                body.target(),
                body.destination(), // REQUIRED now
                ip,
                ua
        ));

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        // ✅ Set cookie only if session was created OR caller had invalid/missing cookie
        if (result.sessionId() != null && (result.sessionCreated() || sessionId == null)) {
            builder.header(HttpHeaders.SET_COOKIE, buildSignupCookie(result.sessionId()).toString());
        }

        return builder.body(new SendSignupOtpResponse(
                result.outcome(),
                result.sent(),
                result.sessionId(),
                result.sessionCreated(),
                result.challengeId(),
                result.ttlMinutes(),
                result.maskedDestination(),
                result.emailVerified(),
                result.phoneVerified(),
                result.emailOtpPending(),
                result.phoneOtpPending(),
                result.state()
        ));
    }

    // -------------------------
    // Cancel session (tolerant: missing/invalid cookie => NO_SESSION-like response)
    // -------------------------
    @PostMapping("/cancel")
    public ResponseEntity<CancelSignupSessionResponse> cancel(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        var result = cancelSignupSessionUseCase.cancel(
                new CancelSignupSessionUseCase.Command(sessionId)
        );

        // ✅ Always delete cookie on cancel endpoint (nice UX)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSignupCookie().toString())
                .body(new CancelSignupSessionResponse(
                        result.sessionId(),
                        result.state()
                ));
    }
}