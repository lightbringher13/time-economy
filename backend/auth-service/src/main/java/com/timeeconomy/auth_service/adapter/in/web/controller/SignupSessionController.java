package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.SignupBootstrapResponse;
import com.timeeconomy.auth_service.adapter.in.web.dto.UpdateSignupProfileRequest;
import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.port.in.GetSignupSessionStatusUseCase;
import com.timeeconomy.auth_service.domain.port.in.UpdateSignupProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class SignupSessionController {

    private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";

    private final GetSignupSessionStatusUseCase getSignupSessionStatusUseCase;
    private final UpdateSignupProfileUseCase updateSignupProfileUseCase;

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
        // 쿠키가 없으면 세션 없음으로 처리
        if (sessionIdValue == null || sessionIdValue.isBlank()) {
            SignupBootstrapResponse empty = new SignupBootstrapResponse(
                    false,
                    null,
                    false,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null
            );
            return ResponseEntity.ok(empty);
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionIdValue);
        } catch (IllegalArgumentException ex) {
            // 잘못된 UUID 형식이면 세션 없음 처리
            SignupBootstrapResponse empty = new SignupBootstrapResponse(
                    false,
                    null,
                    false,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null
            );
            return ResponseEntity.ok(empty);
        }

        var result = getSignupSessionStatusUseCase.getStatus(
                new GetSignupSessionStatusUseCase.Query(sessionId)
        );

        if (!result.exists()) {
            SignupBootstrapResponse empty = new SignupBootstrapResponse(
                    false,
                    null,
                    false,
                    null,
                    false,
                    null,
                    null,
                    null,
                    result.state() != null ? result.state().name() : null
            );
            return ResponseEntity.ok(empty);
        }

        SignupBootstrapResponse response = new SignupBootstrapResponse(
                true,
                result.email(),
                result.emailVerified(),
                result.phoneNumber(),
                result.phoneVerified(),
                result.name(),
                result.gender(),
                result.birthDate(),
                result.state() != null ? result.state().name() : null
        );

        return ResponseEntity.ok(response);
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
                        body.name(),
                        body.phoneNumber(),
                        body.gender(),
                        body.birthDate()
                )
        );

        return ResponseEntity.noContent().build();
    }
}