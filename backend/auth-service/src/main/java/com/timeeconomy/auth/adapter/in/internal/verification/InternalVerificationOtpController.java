package com.timeeconomy.auth.adapter.in.internal.verification;

import com.timeeconomy.auth.domain.verification.port.in.internal.GetVerificationOtpOnceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/verification")
public class InternalVerificationOtpController {

    private final GetVerificationOtpOnceUseCase getVerificationOtpOnceUseCase;

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

        try {
            GetVerificationOtpOnceUseCase.Result result =
                    getVerificationOtpOnceUseCase.getOnce(
                            new GetVerificationOtpOnceUseCase.Command(challengeId)
                    );

            return ResponseEntity.ok(Map.of(
                    "verificationChallengeId", result.verificationChallengeId(),
                    "otp", result.otp()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}