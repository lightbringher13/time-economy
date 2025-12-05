package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.domain.port.in.RequestPhoneVerificationUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyPhoneCodeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final RequestPhoneVerificationUseCase requestPhoneVerificationUseCase;
    private final VerifyPhoneCodeUseCase verifyPhoneCodeUseCase;

    /**
     * 인증번호 발송 요청
     * 현재는 실제 SMS 연동 없이 코드만 생성해서 로그로 남김.
     */
    @PostMapping("/request-code")
    public ResponseEntity<Void> requestCode(
            @Valid @RequestBody RequestPhoneVerificationCodeRequest request) {
        RequestPhoneVerificationUseCase.RequestCommand command = new RequestPhoneVerificationUseCase.RequestCommand(
                request.getPhoneNumber(),
                request.getCountryCode());

        requestPhoneVerificationUseCase.requestVerification(command);
        return ResponseEntity.ok().build();
    }

    /**
     * 인증번호 검증
     */
    @PostMapping("/verify")
    public ResponseEntity<VerifyPhoneCodeResponse> verify(
            @Valid @RequestBody VerifyPhoneCodeRequest request) {
        VerifyPhoneCodeUseCase.VerifyCommand command = new VerifyPhoneCodeUseCase.VerifyCommand(
                request.getPhoneNumber(),
                request.getCode());

        VerifyPhoneCodeUseCase.Result result = verifyPhoneCodeUseCase.verify(command);

        return ResponseEntity.ok(new VerifyPhoneCodeResponse(result.success()));
    }

    // === DTOs ===

    @Getter
    @Setter
    public static class RequestPhoneVerificationCodeRequest {

        @NotBlank
        private String phoneNumber;

        // Optional, default handled in usecase (+82)
        private String countryCode;
    }

    @Getter
    @Setter
    public static class VerifyPhoneCodeRequest {

        @NotBlank
        private String phoneNumber;

        @NotBlank
        private String code;
    }

    @Getter
    public static class VerifyPhoneCodeResponse {

        private final boolean success;

        public VerifyPhoneCodeResponse(boolean success) {
            this.success = success;
        }
    }
}