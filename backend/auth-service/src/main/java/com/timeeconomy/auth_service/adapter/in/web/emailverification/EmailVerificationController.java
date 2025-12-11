package com.timeeconomy.auth_service.adapter.in.web.emailverification;

import com.timeeconomy.auth_service.adapter.in.web.emailverification.dto.request.SendEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.emailverification.dto.request.VerifyEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.emailverification.dto.response.EmailVerificationStatusResponse;
import com.timeeconomy.auth_service.adapter.in.web.emailverification.dto.response.VerifyEmailCodeResponse;
import com.timeeconomy.auth_service.domain.emailverification.port.in.GetEmailVerificationStatusUseCase;
import com.timeeconomy.auth_service.domain.emailverification.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.emailverification.port.in.VerifyEmailCodeUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

        private final SendEmailVerificationCodeUseCase sendEmailVerificationCodeUseCase;
        private final GetEmailVerificationStatusUseCase getEmailVerificationStatusUseCase;
        private final VerifyEmailCodeUseCase verifyEmailCodeUseCase;

        @PostMapping("/send-code")
        public ResponseEntity<Void> sendCode(@RequestBody SendEmailCodeRequest request) {

        var cmd = new SendEmailVerificationCodeUseCase.SendCommand(request.email());
        sendEmailVerificationCodeUseCase.send(cmd);
        return ResponseEntity.ok().build();

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