package com.timeeconomy.auth.adapter.in.internal.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.timeeconomy.auth.adapter.in.internal.verification.dto.response.InternalVerificationOtpResponse;
import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationOtpDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationOtpUseCase;

@RestController
@RequestMapping("/internal/verification")
public class InternalVerificationOtpController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final PeekVerificationOtpUseCase peekVerificationOtpUseCase;
    private final AcknowledgeVerificationOtpDeliveryUseCase acknowledgeVerificationOtpDeliveryUseCase;
    private final byte[] expectedInternalToken;

    public InternalVerificationOtpController(
            PeekVerificationOtpUseCase peekVerificationOtpUseCase,
            AcknowledgeVerificationOtpDeliveryUseCase acknowledgeVerificationOtpDeliveryUseCase,
            @Value("${app.internal.token}") String internalToken
    ) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("app.internal.token must not be blank");
        }
        this.peekVerificationOtpUseCase = peekVerificationOtpUseCase;
        this.acknowledgeVerificationOtpDeliveryUseCase = acknowledgeVerificationOtpDeliveryUseCase;
        this.expectedInternalToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/challenges/{challengeId}/otp")
    public ResponseEntity<InternalVerificationOtpResponse> peekOtp(
            @PathVariable String challengeId,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return forbidden();
        }

        try {
            PeekVerificationOtpUseCase.Result result = peekVerificationOtpUseCase.peek(
                    new PeekVerificationOtpUseCase.Command(challengeId)
            );

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(new InternalVerificationOtpResponse(
                            result.verificationChallengeId(),
                            result.otp()
                    ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .build();
        }
    }

    @DeleteMapping("/challenges/{challengeId}/otp")
    public ResponseEntity<Void> acknowledgeOtpDelivery(
            @PathVariable String challengeId,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return forbidden();
        }

        acknowledgeVerificationOtpDeliveryUseCase.acknowledge(
                new AcknowledgeVerificationOtpDeliveryUseCase.Command(challengeId)
        );

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    private boolean isAuthorized(String token) {
        return token != null && MessageDigest.isEqual(
                expectedInternalToken,
                token.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static <T> ResponseEntity<T> forbidden() {
        return ResponseEntity.status(403)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }
}
