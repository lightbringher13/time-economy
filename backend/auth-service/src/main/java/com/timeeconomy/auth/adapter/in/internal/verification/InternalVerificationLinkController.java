package com.timeeconomy.auth.adapter.in.internal.verification;

import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.in.internal.GetVerificationLinkUrlOnceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/verification")
public class InternalVerificationLinkController {

    private final GetVerificationLinkUrlOnceUseCase useCase;

    @Value("${app.internal.token}")
    private String internalToken;

    @GetMapping("/challenges/{challengeId}/link-url")
    public ResponseEntity<?> getLinkUrlOnce(
            @PathVariable String challengeId,
            @RequestParam("purpose") VerificationPurpose purpose,
            @RequestHeader("X-Internal-Token") String token,
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        log.warn("[INTERNAL] link-url request challengeId={} purpose={} eventId={} ua={}",
                challengeId, purpose, eventId, userAgent);

        if (token == null || !token.equals(internalToken)) {
            log.warn("[INTERNAL] link-url forbidden challengeId={} purpose={} eventId={}",
                    challengeId, purpose, eventId);
            return ResponseEntity.status(403).build();
        }

        try {
            var result = useCase.getOnce(new GetVerificationLinkUrlOnceUseCase.Command(challengeId, purpose));

            log.warn("[INTERNAL] link-url OK challengeId={} purpose={} eventId={}",
                    challengeId, purpose, eventId);

            return ResponseEntity.ok(Map.of(
                    "verificationChallengeId", result.verificationChallengeId(),
                    "linkUrl", result.linkUrl()
            ));

        } catch (NoSuchElementException e) {
            log.warn("[INTERNAL] link-url NOT_FOUND challengeId={} purpose={} eventId={}",
                    challengeId, purpose, eventId);
            return ResponseEntity.notFound().build();
        }
    }
}