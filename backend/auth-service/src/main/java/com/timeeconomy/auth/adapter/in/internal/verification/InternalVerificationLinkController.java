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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.timeeconomy.auth.adapter.in.internal.verification.dto.response.InternalVerificationLinkResponse;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationLinkDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationLinkUrlUseCase;

@RestController
@RequestMapping("/internal/verification")
public class InternalVerificationLinkController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final PeekVerificationLinkUrlUseCase peekVerificationLinkUrlUseCase;
    private final AcknowledgeVerificationLinkDeliveryUseCase acknowledgeVerificationLinkDeliveryUseCase;
    private final byte[] expectedInternalToken;

    public InternalVerificationLinkController(
            PeekVerificationLinkUrlUseCase peekVerificationLinkUrlUseCase,
            AcknowledgeVerificationLinkDeliveryUseCase acknowledgeVerificationLinkDeliveryUseCase,
            @Value("${app.internal.token}") String internalToken
    ) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("app.internal.token must not be blank");
        }
        this.peekVerificationLinkUrlUseCase = peekVerificationLinkUrlUseCase;
        this.acknowledgeVerificationLinkDeliveryUseCase = acknowledgeVerificationLinkDeliveryUseCase;
        this.expectedInternalToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/challenges/{challengeId}/link-url")
    public ResponseEntity<InternalVerificationLinkResponse> peekLinkUrl(
            @PathVariable String challengeId,
            @RequestParam("purpose") VerificationPurpose purpose,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return forbidden();
        }

        try {
            PeekVerificationLinkUrlUseCase.Result result = peekVerificationLinkUrlUseCase.peek(
                    new PeekVerificationLinkUrlUseCase.Command(challengeId, purpose)
            );

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(new InternalVerificationLinkResponse(
                            result.verificationChallengeId(),
                            result.linkUrl()
                    ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .build();
        }
    }

    @DeleteMapping("/challenges/{challengeId}/link-url")
    public ResponseEntity<Void> acknowledgeLinkDelivery(
            @PathVariable String challengeId,
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return forbidden();
        }

        acknowledgeVerificationLinkDeliveryUseCase.acknowledge(
                new AcknowledgeVerificationLinkDeliveryUseCase.Command(challengeId)
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
