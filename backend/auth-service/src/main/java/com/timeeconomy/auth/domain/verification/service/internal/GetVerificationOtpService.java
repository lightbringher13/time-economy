package com.timeeconomy.auth.domain.verification.service.internal;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.verification.port.in.internal.GetVerificationOtpOnceUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVerificationOtpService implements GetVerificationOtpOnceUseCase {

    private final VerificationChallengeRepositoryPort repo;

    @Override
    public Result getOnce(Command command) {
        String otp = repo.getAndDelete(command.challengeId()) // ✅ PEEK (non-destructive)
                .orElseThrow(() -> new NoSuchElementException("otp not found"));

        return new Result(command.challengeId(), otp);
    }
}