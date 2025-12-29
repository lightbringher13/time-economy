package com.timeeconomy.auth.adapter.in.redis.internal;

import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/verification")
public class InternalVerificationOtpController {

    private final VerificationChallengeRepositoryPort verificationChallengeRepositoryPort;

    @Value("${app.internal.token}")
    private String internalToken;

    @GetMapping("/challenges/{challengeId}/otp")
    public ResponseEntity<?> getOtpOnce(
            @PathVariable String challengeId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        if (token == null || !token.equals(internalToken)) {
            return ResponseEntity.status(403).build();
        }

        return verificationChallengeRepositoryPort.getAndDelete(challengeId)
                .<ResponseEntity<?>>map(otp -> ResponseEntity.ok(Map.of(
                        "verificationChallengeId", challengeId,
                        "otp", otp
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}