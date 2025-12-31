package com.timeeconomy.auth.adapter.in.web.verification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.verification.dto.request.ConsumeRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.CreateOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.VerifyOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.CreateOtpResponse;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.VerifyOtpResponse;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.ConsumeVerificationUseCase;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;

import java.time.Duration;

@RestController
@RequestMapping("/api/protected/verification")
@RequiredArgsConstructor
public class ProtectedVerificationController {

    private final CreateOtpUseCase createOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ConsumeVerificationUseCase consumeVerificationUseCase;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

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
                DEFAULT_TTL,
                DEFAULT_MAX_ATTEMPTS,
                clientIp(http),
                userAgent(http)
        );

        var result = createOtpUseCase.createOtp(cmd);

        return ResponseEntity.ok(new CreateOtpResponse(
                result.challengeId(),
                result.sent(),
                result.ttlMinutes(),
                result.maskedDestination()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        var cmd = new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                request.purpose(),
                request.channel(),
                request.destination(),
                request.code()
        );

        var result = verifyOtpUseCase.verifyOtp(cmd);
        return ResponseEntity.ok(new VerifyOtpResponse(result.success()));
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@RequestBody ConsumeRequest req) {
        consumeVerificationUseCase.consume(new ConsumeVerificationUseCase.ConsumeCommand(req.challengeId()));
        return ResponseEntity.ok().build();
    }

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