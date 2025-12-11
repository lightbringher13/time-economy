package com.timeeconomy.auth_service.adapter.in.web.changeemail;

import com.timeeconomy.auth_service.domain.port.in.RequestEmailChangeUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifySecondFactorUseCase;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request.RequestEmailChangeRequest;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request.VerifyNewEmailCodeRequest;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request.VerifySecondFactorRequest;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response.RequestEmailChangeResponse;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response.VerifyNewEmailCodeResponse;
import com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response.VerifySecondFactorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email-change")
@RequiredArgsConstructor
public class ChangeEmailController {

    private final RequestEmailChangeUseCase requestEmailChangeUseCase;
    private final VerifyNewEmailCodeUseCase verifyNewEmailCodeUseCase;
    private final VerifySecondFactorUseCase verifySecondFactorUseCase;

    // 1) Start change-email flow (password + new email)
    @PostMapping("/request")
    public ResponseEntity<RequestEmailChangeResponse> requestEmailChange(
            @RequestHeader("X-User-Id") Long userId,  // ⬅ from gateway
            @RequestBody RequestEmailChangeRequest request
    ) {
        var result = requestEmailChangeUseCase.requestEmailChange(
                new RequestEmailChangeUseCase.RequestEmailChangeCommand(
                        userId,
                        request.currentPassword(),
                        request.newEmail()
                )
        );

        return ResponseEntity.ok(
                new RequestEmailChangeResponse(
                        result.requestId(),
                        result.maskedNewEmail()
                )
        );
    }

    // 2) Verify code sent to NEW email, send 2nd-factor (phone or old email)
    @PostMapping("/verify-new-email")
    public ResponseEntity<VerifyNewEmailCodeResponse> verifyNewEmailCode(
            @RequestHeader("X-User-Id") Long userId,  // ⬅ from gateway
            @RequestBody VerifyNewEmailCodeRequest request
    ) {
        var result = verifyNewEmailCodeUseCase.verifyNewEmailCode(
                new VerifyNewEmailCodeUseCase.VerifyNewEmailCodeCommand(
                        userId,
                        request.requestId(),
                        request.code()
                )
        );

        return ResponseEntity.ok(
                new VerifyNewEmailCodeResponse(
                        result.requestId(),
                        result.secondFactorType()
                )
        );
    }

    // 3) Verify 2nd factor (phone / old email) and COMMIT email change
    @PostMapping("/verify-second-factor")
    public ResponseEntity<VerifySecondFactorResponse> verifySecondFactorAndCommit(
            @RequestHeader("X-User-Id") Long userId,  // ⬅ from gateway
            @RequestBody VerifySecondFactorRequest request
    ) {
        var result = verifySecondFactorUseCase.verifySecondFactorAndCommit(
                new VerifySecondFactorUseCase.VerifySecondFactorCommand(
                        userId,
                        request.requestId(),
                        request.code()
                )
        );

        return ResponseEntity.ok(
                new VerifySecondFactorResponse(
                        result.requestId(),
                        result.newEmail()
                )
        );
    }
}