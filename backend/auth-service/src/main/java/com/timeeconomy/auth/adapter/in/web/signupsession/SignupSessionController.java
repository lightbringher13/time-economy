package com.timeeconomy.auth.adapter.in.web.signupsession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.UpdateSignupProfileResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.VerifySignupOtpResponse;
import com.timeeconomy.auth.adapter.in.web.common.dto.response.ApiErrorResponse;
import com.timeeconomy.auth.adapter.in.web.common.dto.response.ApiErrorDetails;

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

        return ResponseEntity.ok(toStatusResponse(result));
    }

    // -------------------------
    // Update profile (requires session)
    // -------------------------
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody UpdateSignupProfileRequest body
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        if (sessionId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiErrorResponse("NO_SESSION", "Your signup session is missing or expired."));
        }

        var result = updateSignupProfileUseCase.updateProfile(
                new UpdateSignupProfileUseCase.Command(
                        sessionId,
                        body.name(),
                        body.gender(),
                        body.birthDate()
                )
        );

        return toUpdateProfileHttp(result); // UPDATED -> UpdateSignupProfileResponse, else -> ApiErrorResponse
    }

    // -------------------------
    // Verify OTP (cookie-only)
    // -------------------------
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @Valid @RequestBody VerifySignupOtpRequest req,
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            HttpServletRequest http
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        var result = verifySignupOtpUseCase.verify(
                new VerifySignupOtpUseCase.Command(
                        sessionId,
                        req.target(),
                        req.code(),
                        clientIp(http)   // ✅ NEW
                )
        );

        return toVerifyOtpHttp(result);
    }

    // -------------------------
    // Send OTP (lazy-open + destination required)
    // -------------------------
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody SendSignupOtpRequest req,
            HttpServletRequest http
    ) {
        UUID cookieSessionId = parseCookieUuidOrNull(sessionIdValue);

        var result = sendSignupOtpUseCase.send(new SendSignupOtpUseCase.Command(
                cookieSessionId,
                req.target(),
                req.destination(),
                clientIp(http),
                http.getHeader("User-Agent")
        ));

        return toSendOtpHttp(cookieSessionId, result);
    }

    // -------------------------
    // Cancel session (tolerant)
    // -------------------------
    @PostMapping("/cancel")
    public ResponseEntity<CancelSignupSessionResponse> cancel(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID sessionId = parseCookieUuidOrNull(sessionIdValue);

        var result = cancelSignupSessionUseCase.cancel(
                new CancelSignupSessionUseCase.Command(sessionId)
        );

        // ✅ Always delete cookie on cancel endpoint
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSignupCookie().toString())
                .body(toCancelResponse(result));
    }

    // =========================================================
    // Mapping helpers (controller-only)
    // =========================================================

    private SignupStatusResponse toStatusResponse(GetSignupSessionStatusUseCase.Result r) {
        return new SignupStatusResponse(
                r.exists(),
                r.email(),
                r.emailVerified(),
                r.emailOtpPending(),
                r.phoneNumber(),
                r.phoneVerified(),
                r.phoneOtpPending(),
                r.name(),
                r.gender(),
                r.birthDate(),
                r.state()
        );
    }

    private SendSignupOtpResponse toSendOtpResponse(SendSignupOtpUseCase.Result r) {
        return new SendSignupOtpResponse(
                r.outcome(),
                r.sent(),
                r.sessionId(),
                r.sessionCreated(),
                r.challengeId(),
                r.ttlMinutes(),
                r.maskedDestination(),

                r.retryAfterSeconds(),      // ✅ NEW

                r.emailOtpExpiresAt(),      // ✅ NEW
                r.phoneOtpExpiresAt(),      // ✅ NEW

                r.emailVerified(),
                r.phoneVerified(),
                r.emailOtpPending(),
                r.phoneOtpPending(),
                r.state()
        );
    }

    private VerifySignupOtpResponse toVerifyOtpResponse(VerifySignupOtpUseCase.Result r) {
        return new VerifySignupOtpResponse(
                r.outcome(),
                r.success(),
                r.sessionId(),
                r.otpExpiresAt(),
                r.remainingAttempts(),
                r.retryAfterSeconds(),
                r.emailVerified(),
                r.phoneVerified(),
                r.emailOtpPending(),
                r.phoneOtpPending(),
                r.state()
        );
    }

    private CancelSignupSessionResponse toCancelResponse(CancelSignupSessionUseCase.Result r) {
        return new CancelSignupSessionResponse(
                r.outcome(),
                r.canceled(),
                r.sessionId(),
                r.state()
        );
    }

    // =========================================================
    // HTTP status mapping (optional but “big-co” nice)
    // =========================================================

    private ResponseEntity<?> toSendOtpHttp(
            UUID cookieSessionId,
            SendSignupOtpUseCase.Result r
    ) {
        // ✅ success outcomes -> return success DTO
        if (r.outcome() == SendSignupOtpUseCase.Outcome.SENT
                || r.outcome() == SendSignupOtpUseCase.Outcome.ALREADY_VERIFIED
                || r.outcome() == SendSignupOtpUseCase.Outcome.THROTTLED) {

            SendSignupOtpResponse body = toSendOtpResponse(r);

            ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
            if (r.sessionId() != null && shouldSetSignupCookie(cookieSessionId, r.sessionId(), r.sessionCreated())) {
                ok.header(HttpHeaders.SET_COOKIE, buildSignupCookie(r.sessionId()).toString());
            }
            return ok.body(body);
        }

        HttpStatus status = switch (r.outcome()) {
            case INVALID_DESTINATION, INVALID_INPUT -> HttpStatus.BAD_REQUEST; // 400
            case EMAIL_ALREADY_USED, PHONE_ALREADY_USED, INVALID_STATE -> HttpStatus.CONFLICT; // 409
            case DELIVERY_FAILED -> HttpStatus.SERVICE_UNAVAILABLE; // ✅ 503
            default -> HttpStatus.BAD_REQUEST;
        };

        String message = switch (r.outcome()) {
            case INVALID_DESTINATION -> "Please enter a valid email/phone.";
            case INVALID_INPUT -> "Invalid request.";
            case INVALID_STATE -> "Complete the previous step first.";
            case EMAIL_ALREADY_USED -> "That email is already in use.";
            case PHONE_ALREADY_USED -> "That phone number is already in use.";
            case DELIVERY_FAILED -> "Failed to send OTP. Please try again."; // ✅
            default -> "Request failed.";
        };

        ApiErrorResponse err = new ApiErrorResponse(r.outcome().name(), message);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);

        // optional: if session got created even on non-success outcomes and you want cookie, keep this
        if (r.sessionId() != null && shouldSetSignupCookie(cookieSessionId, r.sessionId(), r.sessionCreated())) {
            builder.header(HttpHeaders.SET_COOKIE, buildSignupCookie(r.sessionId()).toString());
        }

        return builder.body(err);
    }

    private ResponseEntity<?> toUpdateProfileHttp(UpdateSignupProfileUseCase.Result r) {

        // ✅ success -> return success DTO
        if (r.outcome() == UpdateSignupProfileUseCase.Outcome.UPDATED) {
            UpdateSignupProfileResponse body = new UpdateSignupProfileResponse(
                    r.outcome(),
                    true,
                    r.sessionId(),
                    r.state()
            );
            return ResponseEntity.ok(body);
        }

        // ❌ expected failures -> ApiErrorResponse
        HttpStatus status = switch (r.outcome()) {
            case NO_SESSION -> HttpStatus.UNAUTHORIZED;     // 401
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;   // 400
            case INVALID_STATE -> HttpStatus.CONFLICT;      // 409
            default -> HttpStatus.BAD_REQUEST;
        };

        String message = switch (r.outcome()) {
            case NO_SESSION -> "Your signup session is missing or expired.";
            case INVALID_INPUT -> "Please check your profile details.";
            case INVALID_STATE -> "You can’t update your profile at this step.";
            default -> "Request failed.";
        };

        ApiErrorResponse err = new ApiErrorResponse(r.outcome().name(), message);
        return ResponseEntity.status(status).body(err);
    }

    private ResponseEntity<?> toVerifyOtpHttp(VerifySignupOtpUseCase.Result r) {

        // ✅ Success
        if (r.outcome() == VerifySignupOtpUseCase.Outcome.VERIFIED) {
            VerifySignupOtpResponse body = toVerifyOtpResponse(r);
            return ResponseEntity.ok(body);
        }

        // ❌ Expected failures -> ApiErrorResponse (with metadata)
        HttpStatus status = switch (r.outcome()) {
            case NO_SESSION -> HttpStatus.UNAUTHORIZED; // 401

            case THROTTLED -> HttpStatus.TOO_MANY_REQUESTS; // 429

            case LOCKED -> HttpStatus.LOCKED; // 423 (or 429 if you prefer)
            case EXPIRED -> HttpStatus.GONE;  // 410

            case WRONG_CODE, NO_PENDING_OTP, INVALID_INPUT -> HttpStatus.BAD_REQUEST; // 400
            default -> HttpStatus.BAD_REQUEST;
        };

        String message = switch (r.outcome()) {
            case WRONG_CODE -> "Incorrect code. Please try again.";
            case NO_SESSION -> "Your signup session is missing or expired. Please start again.";
            case NO_PENDING_OTP -> "No verification is pending. Please request a new code.";
            case INVALID_INPUT -> "Invalid request.";
            case EXPIRED -> "This code has expired. Please request a new code.";
            case LOCKED -> "Too many attempts. Please request a new code.";
            case THROTTLED -> "Too many requests. Please try again later.";
            default -> "Verification failed.";
        };

        // ✅ Optionally set Retry-After header for THROTTLED
        if (r.outcome() == VerifySignupOtpUseCase.Outcome.THROTTLED && r.retryAfterSeconds() != null) {
            return ResponseEntity.status(status)
                    .header("Retry-After", String.valueOf(r.retryAfterSeconds()))
                    .body(new ApiErrorResponse(
                            r.outcome().name(),
                            message,
                            new ApiErrorDetails(
                                    r.otpExpiresAt(),
                                    r.remainingAttempts(),
                                    r.retryAfterSeconds()
                            )
                    ));
        }

        return ResponseEntity.status(status).body(new ApiErrorResponse(
                r.outcome().name(),
                message,
                new ApiErrorDetails(
                        r.otpExpiresAt(),
                        r.remainingAttempts(),
                        r.retryAfterSeconds()
                )
        ));
    }

    // =========================================================
    // Cookie + request helpers
    // =========================================================

    private UUID parseCookieUuidOrNull(String sessionIdValue) {
        if (sessionIdValue == null || sessionIdValue.isBlank()) return null;
        try {
            return UUID.fromString(sessionIdValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean shouldSetSignupCookie(UUID cookieSessionId, UUID resultSessionId, boolean sessionCreated) {
        if (resultSessionId == null) return false;
        return sessionCreated || cookieSessionId == null;
    }

    private ResponseCookie buildSignupCookie(UUID sessionId) {
        return ResponseCookie.from(SIGNUP_SESSION_COOKIE, sessionId.toString())
                .httpOnly(true)
                .path("/")
                .secure(true)
                .sameSite("Strict") // TODO: env controlled (dev might prefer Lax)
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
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();

        String xri = http.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return http.getRemoteAddr();
    }
}