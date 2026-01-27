package com.timeeconomy.auth.adapter.in.web.verification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import com.timeeconomy.auth.adapter.in.web.verification.dto.request.ConsumeRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.CreateOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.VerifyOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.CreateOtpResponse;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.VerifyOtpResponse;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
// import com.timeeconomy.auth.domain.verification.port.in.ConsumeVerificationUseCase;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;

@RestController
@RequestMapping("/api/protected/verification")
@RequiredArgsConstructor
public class ProtectedVerificationController {

    private final CreateOtpUseCase createOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    // private final ConsumeVerificationUseCase consumeVerificationUseCase;

    /**
     * Protected OTP create (ex: CHANGE_EMAIL).
     * subject = USER(userId from gateway)
     */
    @PostMapping("/otp")
    public ResponseEntity<CreateOtpResponse> createOtp(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOtpRequest request,
            HttpServletRequest http
    ) {
        var cmd = new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                request.purpose(),
                request.channel(),
                request.destination(),
                clientIp(http),
                userAgent(http)
        );

        var result = createOtpUseCase.createOtp(cmd);

        return ResponseEntity.ok(new CreateOtpResponse(
                result.challengeId(),
                result.sent(),
                result.expiresAt(),
                result.maskedDestination()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
        @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest http
    ) {
        var cmd = new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                request.purpose(),
                request.channel(),
                request.destination(),
                request.code(),
                clientIp(http) // ✅ NEW
        );

        var result = verifyOtpUseCase.verifyOtp(cmd);

        return ResponseEntity.ok(new VerifyOtpResponse(
                result.success(),
                result.outcome().name(),
                result.expiresAt(),
                result.remainingAttempts(),
                result.retryAfterSeconds()
        ));
    }

    // @PostMapping("/consume")
    // public ResponseEntity<Void> consume(@RequestBody ConsumeRequest req) {
    //     consumeVerificationUseCase.consume(new ConsumeVerificationUseCase.ConsumeCommand(req.challengeId()));
    //     return ResponseEntity.ok().build();
    // }

    private String userAgent(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }


    
}