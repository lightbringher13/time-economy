package com.timeeconomy.auth.adapter.in.web.changeemail;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.changeemail.dto.request.RequestEmailChangeRequest;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.request.VerifyNewEmailCodeRequest;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.request.VerifySecondFactorRequest;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.request.StartSecondFactorRequest;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.request.CommitEmailChangeRequest;

import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.RequestEmailChangeResponse;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.VerifyNewEmailCodeResponse;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.VerifySecondFactorResponse;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.StartSecondFactorResponse;
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.CommitEmailChangeResponse;

import com.timeeconomy.auth.domain.changeemail.port.in.RequestEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.StartSecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifySecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.CommitEmailChangeUseCase;

@RestController
@RequestMapping("/api/auth/email-change")
@RequiredArgsConstructor
public class ChangeEmailController {

    private final RequestEmailChangeUseCase requestEmailChangeUseCase;
    private final VerifyNewEmailCodeUseCase verifyNewEmailCodeUseCase;
    private final StartSecondFactorUseCase startSecondFactorUseCase;
    private final VerifySecondFactorUseCase verifySecondFactorUseCase;
    private final CommitEmailChangeUseCase commitEmailChangeUseCase;

    // 1) Start change-email flow (password + new email)
    @PostMapping("/request")
    public ResponseEntity<RequestEmailChangeResponse> requestEmailChange(
            @RequestHeader("X-User-Id") Long userId,
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

    // 2) Verify code sent to NEW email (verify-only)
    @PostMapping("/verify-new-email")
    public ResponseEntity<VerifyNewEmailCodeResponse> verifyNewEmailCode(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody VerifyNewEmailCodeRequest request
    ) {
        var result = verifyNewEmailCodeUseCase.verifyNewEmailCode(
                new VerifyNewEmailCodeUseCase.VerifyNewEmailCodeCommand(
                        userId,
                        request.requestId(),
                        request.code()
                )
        );

        return ResponseEntity.ok(new VerifyNewEmailCodeResponse(result.requestId()));
    }

    // 3) Start second factor (send OTP to phone OR old email)
    @PostMapping("/start-second-factor")
    public ResponseEntity<StartSecondFactorResponse> startSecondFactor(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody StartSecondFactorRequest request
    ) {
        var result = startSecondFactorUseCase.startSecondFactor(
                new StartSecondFactorUseCase.StartSecondFactorCommand(
                        userId,
                        request.requestId()
                )
        );

        return ResponseEntity.ok(
                new StartSecondFactorResponse(
                        result.requestId(),
                        result.secondFactorType()
                )
        );
    }

    // 4) Verify second factor (verify-only)
    @PostMapping("/verify-second-factor")
    public ResponseEntity<VerifySecondFactorResponse> verifySecondFactor(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody VerifySecondFactorRequest request
    ) {
        var result = verifySecondFactorUseCase.verifySecondFactor(
                new VerifySecondFactorUseCase.VerifySecondFactorCommand(
                        userId,
                        request.requestId(),
                        request.code()
                )
        );

        return ResponseEntity.ok(new VerifySecondFactorResponse(result.requestId()));
    }

    // 5) Commit the email change + outbox event
    @PostMapping("/commit")
    public ResponseEntity<CommitEmailChangeResponse> commit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CommitEmailChangeRequest request
    ) {
        var result = commitEmailChangeUseCase.commit(
                new CommitEmailChangeUseCase.CommitEmailChangeCommand(
                        userId,
                        request.requestId()
                )
        );

        return ResponseEntity.ok(
                new CommitEmailChangeResponse(
                        result.requestId(),
                        result.newEmail()
                )
        );
    }
}