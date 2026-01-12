package com.timeeconomy.auth.adapter.in.web.signupsession;

import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.SendSignupOtpRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.UpdateSignupProfileRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.VerifySignupOtpRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.SendSignupOtpResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.SignupBootstrapResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.VerifySignupOtpResponse;

import com.timeeconomy.auth.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.SignupBootstrapUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.VerifySignupOtpUseCase;

// ✅ new usecases
import com.timeeconomy.auth.domain.signupsession.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.EditSignupEmailUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.EditSignupPhoneUseCase;
import com.timeeconomy.auth.domain.signupsession.port.in.CancelSignupSessionUseCase;

// ✅ you likely want new response DTOs for these:
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.EditSignupEmailRequest;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.request.EditSignupPhoneRequest;

import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.SignupStatusResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.EditSignupEmailResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.EditSignupPhoneResponse;
import com.timeeconomy.auth.adapter.in.web.signupsession.dto.response.CancelSignupSessionResponse;

@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class SignupSessionController {

    private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";

    private final SignupBootstrapUseCase signupBootstrapUseCase;
    private final UpdateSignupProfileUseCase updateSignupProfileUseCase;
    private final VerifySignupOtpUseCase verifySignupOtpUseCase;
    private final SendSignupOtpUseCase sendSignupOtpUseCase;

    // ✅ new
    private final GetSignupSessionStatusUseCase getSignupSessionStatusUseCase;
    private final EditSignupEmailUseCase editSignupEmailUseCase;
    private final EditSignupPhoneUseCase editSignupPhoneUseCase;
    private final CancelSignupSessionUseCase cancelSignupSessionUseCase;

    // -------------------------
    // helpers
    // -------------------------
    private UUID requireCookieSessionId(String sessionIdValue) {
        if (sessionIdValue == null || sessionIdValue.isBlank()) {
            throw new SignupSessionNotFoundException("Signup session cookie not found");
        }
        try {
            return UUID.fromString(sessionIdValue);
        } catch (IllegalArgumentException ex) {
            throw new SignupSessionNotFoundException("Invalid signup session id in cookie");
        }
    }

    // -------------------------
    // Bootstrap (existing)
    // -------------------------
    @GetMapping("/bootstrap")
    public ResponseEntity<SignupBootstrapResponse> bootstrap(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID existingSessionId = null;
        if (sessionIdValue != null && !sessionIdValue.isBlank()) {
            try { existingSessionId = UUID.fromString(sessionIdValue); }
            catch (IllegalArgumentException ignored) {}
        }

        var result = signupBootstrapUseCase.bootstrap(new SignupBootstrapUseCase.Command(existingSessionId));

        ResponseCookie cookie = ResponseCookie.from(SIGNUP_SESSION_COOKIE, result.sessionId().toString())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();

        SignupBootstrapResponse body = new SignupBootstrapResponse(
                result.exists(),
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    // -------------------------
    // ✅ Status (new)
    // -------------------------
    @GetMapping("/status")
    public ResponseEntity<SignupStatusResponse> status(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID sessionId = requireCookieSessionId(sessionIdValue);

        var result = getSignupSessionStatusUseCase.getStatus(new GetSignupSessionStatusUseCase.Query(sessionId));

        // if you want: if (!result.exists()) return 204
        return ResponseEntity.ok(new SignupStatusResponse(
                result.exists(),
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        ));
    }

    // -------------------------
    // Update profile (existing)
    // -------------------------
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @RequestBody UpdateSignupProfileRequest body
    ) {
        UUID sessionId = requireCookieSessionId(sessionIdValue);

        updateSignupProfileUseCase.updateProfile(
                new UpdateSignupProfileUseCase.Command(
                        sessionId,
                        body.email(),
                        body.name(),
                        body.phoneNumber(),
                        body.gender(),
                        body.birthDate()
                )
        );

        return ResponseEntity.noContent().build();
    }

    // -------------------------
    // Verify OTP (existing)
    // -------------------------
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifySignupOtpResponse> verifyOtp(
            @Valid @RequestBody VerifySignupOtpRequest req,
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String cookieSessionId
    ) {
        UUID sessionId = req.sessionId();

        if (sessionId == null && cookieSessionId != null && !cookieSessionId.isBlank()) {
            try { sessionId = UUID.fromString(cookieSessionId); }
            catch (Exception ignored) {}
        }

        if (sessionId == null) {
            return ResponseEntity.badRequest().build();
        }

        var result = verifySignupOtpUseCase.verify(
                new VerifySignupOtpUseCase.Command(sessionId, req.target(), req.code())
        );

        return ResponseEntity.ok(new VerifySignupOtpResponse(
                result.success(),
                result.sessionId(),
                result.emailVerified(),
                result.phoneVerified(),
                result.state()
        ));
    }

    // -------------------------
    // Send OTP (existing)
    // -------------------------
    @PostMapping("/send-otp")
    public ResponseEntity<SendSignupOtpResponse> sendOtp(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) UUID sessionId,
            @Valid @RequestBody SendSignupOtpRequest body,
            HttpServletRequest http
    ) {
        if (sessionId == null) {
            return ResponseEntity.ok(new SendSignupOtpResponse(false, null, null, 0, null, false, false, "EXPIRED_OR_NOT_FOUND"));
        }

        var result = sendSignupOtpUseCase.send(new SendSignupOtpUseCase.Command(sessionId, body.target()));

        return ResponseEntity.ok(new SendSignupOtpResponse(
                result.sent(),
                result.sessionId(),
                result.challengeId(),
                result.ttlMinutes(),
                result.maskedDestination(),
                result.emailVerified(),
                result.phoneVerified(),
                result.state()
        ));
    }

    // -------------------------
    // ✅ Edit email (new)
    // -------------------------
    @PostMapping("/edit-email")
    public ResponseEntity<EditSignupEmailResponse> editEmail(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody EditSignupEmailRequest body
    ) {
        UUID sessionId = requireCookieSessionId(sessionIdValue);

        var result = editSignupEmailUseCase.editEmail(
                new EditSignupEmailUseCase.Command(sessionId, body.newEmail())
        );

        return ResponseEntity.ok(new EditSignupEmailResponse(
                result.sessionId(),
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        ));
    }

    // -------------------------
    // ✅ Edit phone (new)
    // -------------------------
    @PostMapping("/edit-phone")
    public ResponseEntity<EditSignupPhoneResponse> editPhone(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @Valid @RequestBody EditSignupPhoneRequest body
    ) {
        UUID sessionId = requireCookieSessionId(sessionIdValue);

        var result = editSignupPhoneUseCase.editPhone(
                new EditSignupPhoneUseCase.Command(sessionId, body.newPhoneNumber())
        );

        return ResponseEntity.ok(new EditSignupPhoneResponse(
                result.sessionId(),
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        ));
    }

    // -------------------------
    // ✅ Cancel session (new)
    // -------------------------
    @PostMapping("/cancel")
    public ResponseEntity<CancelSignupSessionResponse> cancel(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID sessionId = requireCookieSessionId(sessionIdValue);

        var result = cancelSignupSessionUseCase.cancel(new CancelSignupSessionUseCase.Command(sessionId));

        // optional: delete cookie as well (nice UX)
        ResponseCookie delete = ResponseCookie.from(SIGNUP_SESSION_COOKIE, "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, delete.toString())
                .body(new CancelSignupSessionResponse(result.sessionId(), result.state()));
    }
}