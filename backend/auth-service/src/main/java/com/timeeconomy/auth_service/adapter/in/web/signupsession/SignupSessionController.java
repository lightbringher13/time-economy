package com.timeeconomy.auth_service.adapter.in.web.signupsession;


import org.springframework.http.HttpHeaders;

import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.request.SendSignupOtpRequest;
import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.request.UpdateSignupProfileRequest;
import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.request.VerifySignupOtpRequest;
import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.response.SendSignupOtpResponse;
import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.response.SignupBootstrapResponse;
import com.timeeconomy.auth_service.adapter.in.web.signupsession.dto.response.VerifySignupOtpResponse;
import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.signupsession.port.in.SendSignupOtpUseCase;
import com.timeeconomy.auth_service.domain.signupsession.port.in.SignupBootstrapUseCase;
import com.timeeconomy.auth_service.domain.signupsession.port.in.UpdateSignupProfileUseCase;
import com.timeeconomy.auth_service.domain.signupsession.port.in.VerifySignupOtpUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class SignupSessionController {

    private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";

    private final SignupBootstrapUseCase signupBootstrapUseCase;
    private final UpdateSignupProfileUseCase updateSignupProfileUseCase;
    private final VerifySignupOtpUseCase verifySignupOtpUseCase;
    private final SendSignupOtpUseCase sendSignupOtpUseCase;


    /**
     * 🔹 Bootstrap endpoint
     *
     * - Reads signup_session_id from HttpOnly cookie
     * - If session exists & active → returns its data
     * - If not → hasSession = false
     */
    @GetMapping("/bootstrap")
    public ResponseEntity<SignupBootstrapResponse> bootstrap(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue
    ) {
        UUID existingSessionId = null;
        if (sessionIdValue != null && !sessionIdValue.isBlank()) {
            try {
                existingSessionId = UUID.fromString(sessionIdValue);
            } catch (IllegalArgumentException ignored) {
                // 잘못된 UUID → 새 세션 만들게 둠
            }
        }

        var result = signupBootstrapUseCase.bootstrap(
                new SignupBootstrapUseCase.Command(existingSessionId)
        );

        // 항상 sessionId를 쿠키로 내려줌 (새로 만들었든 재사용하든)
        ResponseCookie cookie = ResponseCookie.from(SIGNUP_SESSION_COOKIE, result.sessionId().toString())
                .httpOnly(true)
                .secure(true)          // 로컬 개발이면 false도 가능
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();

        SignupBootstrapResponse body = new SignupBootstrapResponse(
                result.exists(),          // hasSession
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state()
        );

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    /**
     * 🔹 Autosave profile endpoint
     *
     * - Uses signup_session_id cookie to find session
     * - Updates name / phone / gender / birthDate in signup_sessions
     * - FE should call this in a debounced way
     */
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String sessionIdValue,
            @RequestBody UpdateSignupProfileRequest body
    ) {
        if (sessionIdValue == null || sessionIdValue.isBlank()) {
            // 쿠키 자체가 없으면 "세션 없음" 예외
            throw new SignupSessionNotFoundException("Signup session cookie not found");
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionIdValue);
        } catch (IllegalArgumentException ex) {
            throw new SignupSessionNotFoundException("Invalid signup session id in cookie");
        }

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

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifySignupOtpResponse> verifyOtp(
            @Valid @RequestBody VerifySignupOtpRequest req,
            @CookieValue(name = "signup_session_id", required = false) String cookieSessionId
    ) {
        UUID sessionId = req.sessionId();

        if (sessionId == null && cookieSessionId != null && !cookieSessionId.isBlank()) {
            try {
                sessionId = UUID.fromString(cookieSessionId);
            } catch (Exception ignored) {
                // keep null -> treat as invalid request
            }
        }

        if (sessionId == null) {
            // BigCom style: 400 for malformed request (missing session id)
            return ResponseEntity.badRequest().build();
        }

        var result = verifySignupOtpUseCase.verify(
                new VerifySignupOtpUseCase.Command(
                        sessionId,
                        req.target(),
                        req.code()
                )
        );

        return ResponseEntity.ok(new VerifySignupOtpResponse(
                result.success(),
                result.sessionId(),
                result.emailVerified(),
                result.phoneVerified(),
                result.state()
        ));
    }

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
}