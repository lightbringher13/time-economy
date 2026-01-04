package com.timeeconomy.auth.adapter.in.web.verification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.verification.dto.request.CreateLinkRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.CreateOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.VerifyLinkRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.request.VerifyOtpRequest;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.CreateLinkResponse;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.CreateOtpResponse;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.VerifyLinkResponse;
import com.timeeconomy.auth.adapter.in.web.verification.dto.response.VerifyOtpResponse;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.in.VerifyLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;


import java.time.Duration;

@RestController
@RequestMapping("/api/auth/public/verification")
@RequiredArgsConstructor
public class PublicVerificationController {

    private final CreateLinkUseCase createLinkUseCase;
    private final VerifyLinkUseCase verifyLinkUseCase;
    private final CreateOtpUseCase createOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;

    // Policy defaults (server decides)
    private static final Duration DEFAULT_OTP_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private static final Duration DEFAULT_LINK_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_LINK_TOKEN_TTL = Duration.ofMinutes(30);

    // =========================
    // OTP
    // =========================

    @PostMapping("/otp")
    public ResponseEntity<CreateOtpResponse> createOtp(
            @Valid @RequestBody CreateOtpRequest request,
            HttpServletRequest http
    ) {
        var cmd = new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.EMAIL,
                request.destination(), // subjectId = email itself
                request.purpose(),
                request.channel(),
                request.destination(),
                DEFAULT_OTP_TTL,
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
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        var cmd = new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.EMAIL,
                request.destination(),
                request.purpose(),
                request.channel(),
                request.destination(),
                request.code()
        );

        var result = verifyOtpUseCase.verifyOtp(cmd);

        return ResponseEntity.ok(new VerifyOtpResponse(result.success()));
    }

    // =========================
    // LINK
    // =========================

    /**
     * Public LINK create (ex: PASSWORD_RESET).
     * subject = EMAIL(email)
     */
    @PostMapping("/link")
    public ResponseEntity<CreateLinkResponse> createLink(
            @Valid @RequestBody CreateLinkRequest request,
            HttpServletRequest http
    ) {
        var cmd = new CreateLinkUseCase.CreateLinkCommand(
                VerificationSubjectType.EMAIL,
                request.destination(), // subjectId = email itself
                request.purpose(),
                request.channel(),
                request.destination(),
                DEFAULT_LINK_TTL,
                DEFAULT_LINK_TOKEN_TTL,
                clientIp(http),
                userAgent(http)
        );

        var result = createLinkUseCase.createLink(cmd);

        return ResponseEntity.ok(new CreateLinkResponse(
                result.challengeId(),
                result.sent(),
                result.ttlMinutes(),
                result.maskedDestination()
        ));
    }

    /**
     * Public LINK verify (token-only).
     * Returns challengeId + destinationNorm so caller can reset password.
     */
    @PostMapping("/link/verify")
    public ResponseEntity<VerifyLinkResponse> verifyLink(
            @Valid @RequestBody VerifyLinkRequest request
    ) {
        var cmd = new VerifyLinkUseCase.VerifyLinkCommand(
                request.purpose(),
                request.channel(),
                request.token()
        );

        var result = verifyLinkUseCase.verifyLink(cmd);

        return ResponseEntity.ok(new VerifyLinkResponse(
                result.success(),
                result.challengeId(),
                result.destinationNorm()
        ));
    }

    // ========= helpers =========

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