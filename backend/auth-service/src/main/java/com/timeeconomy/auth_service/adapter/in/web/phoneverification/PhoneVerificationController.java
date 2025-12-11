package com.timeeconomy.auth_service.adapter.in.web.phoneverification;

import com.timeeconomy.auth_service.adapter.in.web.phoneverification.dto.request.RequestPhoneVerificationCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.phoneverification.dto.request.VerifyPhoneCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.phoneverification.dto.response.VerifyPhoneCodeResponse;
import com.timeeconomy.auth_service.domain.phoneverification.port.in.RequestPhoneVerificationUseCase;
import com.timeeconomy.auth_service.domain.phoneverification.port.in.VerifyPhoneCodeUseCase;

import jakarta.validation.Valid;
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
                request.phoneNumber(),
                request.countryCode());

        requestPhoneVerificationUseCase.requestVerification(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPhoneCodeResponse> verify(
            @Valid @RequestBody VerifyPhoneCodeRequest request,
            @CookieValue(name = "signup_session_id", required = false) String signupSessionIdValue // TODO: replace with constant if you have one
    ) {
        java.util.UUID signupSessionId = null;
        if (signupSessionIdValue != null && !signupSessionIdValue.isBlank()) {
            try {
                signupSessionId = java.util.UUID.fromString(signupSessionIdValue);
            } catch (IllegalArgumentException ex) {
                // 잘못된 UUID 포맷이면 그냥 null 취급 (best effort)
            }
        }

        VerifyPhoneCodeUseCase.VerifyCommand command =
                new VerifyPhoneCodeUseCase.VerifyCommand(
                        request.phoneNumber(),
                        request.code(),
                        signupSessionId
                );

        VerifyPhoneCodeUseCase.Result result = verifyPhoneCodeUseCase.verify(command);

        return ResponseEntity.ok(new VerifyPhoneCodeResponse(result.success()));
    }  
}