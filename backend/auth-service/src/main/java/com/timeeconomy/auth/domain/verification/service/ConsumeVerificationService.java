package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.port.in.ConsumeVerificationUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ConsumeVerificationService implements ConsumeVerificationUseCase {

    private final VerificationChallengeRepositoryPort repo;

    private final java.time.Clock clock;

     @Override
    @Transactional
    public void consume(ConsumeCommand command) {
        Instant now = Instant.now(clock);

        VerificationChallenge ch = repo.findById(command.challengeId())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + command.challengeId()));

        ch.consume(now);
        repo.save(ch);
    }
}