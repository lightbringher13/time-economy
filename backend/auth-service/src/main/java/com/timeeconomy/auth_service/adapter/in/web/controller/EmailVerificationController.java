package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.EmailVerificationStatusResponse;
import com.timeeconomy.auth_service.adapter.in.web.dto.SendEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.VerifyEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.VerifyEmailCodeResponse;
import com.timeeconomy.auth_service.domain.port.in.GetEmailVerificationStatusUseCase;
import com.timeeconomy.auth_service.domain.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyEmailCodeUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

        private final SendEmailVerificationCodeUseCase sendEmailVerificationCodeUseCase;
        private final GetEmailVerificationStatusUseCase getEmailVerificationStatusUseCase;
        private final VerifyEmailCodeUseCase verifyEmailCodeUseCase;

        private static final String SIGNUP_SESSION_COOKIE = "signup_session_id";

        @PostMapping("/send-code")
        public ResponseEntity<Void> sendCode(
                @RequestBody SendEmailCodeRequest request,
                @CookieValue(name = SIGNUP_SESSION_COOKIE, required = false) String signupSessionCookie
        ) {
        // 1) 쿠키에서 기존 세션 ID 파싱
        UUID existingSessionId = null;
        if (signupSessionCookie != null && !signupSessionCookie.isBlank()) {
                try {
                existingSessionId = UUID.fromString(signupSessionCookie);
                } catch (IllegalArgumentException ignored) {
                // 잘못된 UUID면 무시하고 새 세션 만들게 둔다
                }
        }

        // 2) UseCase 호출 (email + existingSessionId)
        var cmd = new SendEmailVerificationCodeUseCase.SendCommand(
                request.email(),
                existingSessionId
        );

        var result = sendEmailVerificationCodeUseCase.send(cmd);

        // 3) HttpOnly signup_session_id 쿠키 설정/갱신
        ResponseCookie cookie = ResponseCookie.from(SIGNUP_SESSION_COOKIE, result.sessionId().toString())
                .httpOnly(true)
                // TODO: 로컬 개발에서는 false, 운영(HTTPS)에서는 true
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();

        // 4) body에는 dev 편의를 위해 code만 내려줌
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
        }

        @PostMapping("/verify")
        public ResponseEntity<VerifyEmailCodeResponse> verify(
                @CookieValue(name = "signup_session_id", required = false) String signupSessionIdCookie,
                @RequestBody VerifyEmailCodeRequest request
        ) {
        UUID signupSessionId = null;

        if (signupSessionIdCookie != null && !signupSessionIdCookie.isBlank()) {
                try {
                signupSessionId = UUID.fromString(signupSessionIdCookie);
                } catch (IllegalArgumentException ex) {
                signupSessionId = null; // invalid cookie → ignore
                }
        }

        var cmd = new VerifyEmailCodeUseCase.VerifyCommand(
                signupSessionId,
                request.email(),
                request.code()
        );

        var result = verifyEmailCodeUseCase.verify(cmd);

        return ResponseEntity.ok(new VerifyEmailCodeResponse(result.success()));
        }

        @GetMapping("/status")
        public ResponseEntity<EmailVerificationStatusResponse> getStatus(
                @RequestParam String email
        ) {
                var result = getEmailVerificationStatusUseCase.getStatus(email);
                return ResponseEntity.ok(new EmailVerificationStatusResponse(result.verified()));
        }
}