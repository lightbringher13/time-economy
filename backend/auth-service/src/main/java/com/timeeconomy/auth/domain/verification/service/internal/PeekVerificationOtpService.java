package com.timeeconomy.auth.domain.verification.service.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PeekVerificationOtpService implements PeekVerificationOtpUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final Clock clock;

    @Override
    public Result peek(Command command) {
        String challengeId = command == null ? null : command.challengeId();
        if (challengeId == null || challengeId.isBlank()) {
            throw new NoSuchElementException("otp not found");
        }

        Instant now = Instant.now(clock);
        VerificationChallenge challenge = repo.findById(challengeId)
                .filter(VerificationChallenge::isOtp)
                .filter(VerificationChallenge::isPending)
                .filter(candidate -> !candidate.isExpired(now))
                .orElseThrow(() -> new NoSuchElementException("otp not found"));

        String otp = repo.peekOtp(challenge.getId())
                .orElseThrow(() -> new NoSuchElementException("otp not found"));

        return new Result(challenge.getId(), otp);
    }
}
