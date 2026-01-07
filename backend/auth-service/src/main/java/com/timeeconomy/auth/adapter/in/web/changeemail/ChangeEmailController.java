package com.timeeconomy.auth.adapter.in.web.changeemail;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

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
import com.timeeconomy.auth.adapter.in.web.changeemail.dto.response.GetEmailChangeStatusResponse;

import com.timeeconomy.auth.domain.changeemail.port.in.RequestEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.StartSecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifySecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.CommitEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.GetEmailChangeStatusUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.GetActiveEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.ResendNewEmailOtpUseCase;
import com.timeeconomy.auth.domain.changeemail.port.in.CancelEmailChangeUseCase;

@RestController
@RequestMapping("/api/auth/email-change")
@RequiredArgsConstructor
public class ChangeEmailController {

    private final RequestEmailChangeUseCase requestEmailChangeUseCase;
    private final VerifyNewEmailCodeUseCase verifyNewEmailCodeUseCase;
    private final StartSecondFactorUseCase startSecondFactorUseCase;
    private final VerifySecondFactorUseCase verifySecondFactorUseCase;
    private final CommitEmailChangeUseCase commitEmailChangeUseCase;
    private final GetEmailChangeStatusUseCase getEmailChangeStatusUseCase;
    private final GetActiveEmailChangeUseCase getActiveEmailChangeUseCase;
    private final ResendNewEmailOtpUseCase resendNewEmailOtpUseCase;
    private final CancelEmailChangeUseCase cancelEmailChangeUseCase;

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
                        result.maskedNewEmail(),
                        result.status()
                )
        );
    }

    // ✅ 1.25) Get my active request (survive refresh without storage)
    @GetMapping("/active")
    public ResponseEntity<GetEmailChangeStatusResponse> getActive(
            @RequestHeader("X-User-Id") Long userId
    ) {
        var opt = getActiveEmailChangeUseCase.getActive(
                new GetActiveEmailChangeUseCase.GetActiveEmailChangeCommand(userId)
        );

        // 204 = no active request
        if (opt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        var result = opt.get();

        return ResponseEntity.ok(
                new GetEmailChangeStatusResponse(
                        result.requestId(),
                        result.status(),
                        result.secondFactorType(),
                        result.maskedNewEmail(),
                        result.expiresAt()
                )
        );
    }

    // 1.5) Get current status (polling endpoint)
    @GetMapping("/{requestId}/status")
    public ResponseEntity<GetEmailChangeStatusResponse> getStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long requestId
    ) {
        var result = getEmailChangeStatusUseCase.getStatus(
                new GetEmailChangeStatusUseCase.GetEmailChangeStatusCommand(userId, requestId)
        );

        return ResponseEntity.ok(
                new GetEmailChangeStatusResponse(
                        result.requestId(),
                        result.status(),
                        result.secondFactorType(),
                        result.maskedNewEmail(),
                        result.expiresAt()
                )
        );
    }

    // 2) Verify code sent to NEW email
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

        return ResponseEntity.ok(
                new VerifyNewEmailCodeResponse(
                        result.requestId(),
                        result.status()
                )
        );
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
                        result.secondFactorType(),
                        result.status()
                )
        );
    }

    // 4) Verify second factor
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

        return ResponseEntity.ok(
                new VerifySecondFactorResponse(
                        result.requestId(),
                        result.status()
                )
        );
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

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
            .body(new CommitEmailChangeResponse(
                        result.requestId(),
                        result.newEmail(),
                        result.status()
                ));
    }
    
    // 2.1) Resend NEW email OTP (only when status=PENDING)
@PostMapping("/{requestId}/resend-new-email-otp")
public ResponseEntity<Void> resendNewEmailOtp(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long requestId
) {
    resendNewEmailOtpUseCase.resend(
            new ResendNewEmailOtpUseCase.ResendCommand(userId, requestId)
    );
    return ResponseEntity.noContent().build();
}

// 0) Cancel current email change request
@PostMapping("/{requestId}/cancel")
public ResponseEntity<Void> cancel(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long requestId
) {
    cancelEmailChangeUseCase.cancel(
            new CancelEmailChangeUseCase.CancelCommand(userId, requestId)
    );
    return ResponseEntity.noContent().build();
}

}