package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.EmailVerificationStatusResponse;
import com.timeeconomy.auth_service.adapter.in.web.dto.SendEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.VerifyEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.VerifyEmailCodeResponse;
import com.timeeconomy.auth_service.domain.port.in.GetEmailVerificationStatusUseCase;
import com.timeeconomy.auth_service.domain.port.in.SendEmailVerificationCodeUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyEmailCodeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

        private final SendEmailVerificationCodeUseCase sendEmailVerificationCodeUseCase;
        private final GetEmailVerificationStatusUseCase getEmailVerificationStatusUseCase;
        private final VerifyEmailCodeUseCase verifyEmailCodeUseCase;

        /**
         * 이메일로 인증 코드 발송
         */
        @PostMapping("/send-code")
        public ResponseEntity<String> sendCode(
                        @RequestBody SendEmailCodeRequest request,
                        HttpServletRequest httpRequest) {
                var cmd = new SendEmailVerificationCodeUseCase.SendCommand(
                                request.email(),
                                httpRequest.getRemoteAddr(),
                                httpRequest.getHeader("User-Agent"));

                String code = sendEmailVerificationCodeUseCase.send(cmd);

                // FE는 204 or 200 만 보고 "코드 보냈다" UI 보여주면 됨
                return ResponseEntity.ok(code);
        }

        /**
         * 이메일 + 코드로 검증
         */
        @PostMapping("/verify")
        public ResponseEntity<VerifyEmailCodeResponse> verify(
                        @RequestBody VerifyEmailCodeRequest request) {
                var cmd = new VerifyEmailCodeUseCase.VerifyCommand(
                                request.email(),
                                request.code());

                var result = verifyEmailCodeUseCase.verify(cmd);

                return ResponseEntity.ok(
                                new VerifyEmailCodeResponse(result.success()));
        }

        @GetMapping("/status")
        public ResponseEntity<EmailVerificationStatusResponse> getStatus(
                @RequestParam String email
        ) {
                var result = getEmailVerificationStatusUseCase.getStatus(email);
                return ResponseEntity.ok(new EmailVerificationStatusResponse(result.verified()));
        }
}