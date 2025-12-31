package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;

import com.timeeconomy.auth.domain.verification.port.in.VerifyLinkUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;


import java.time.Instant;


@Service
@RequiredArgsConstructor
public class VerifyLinkService implements VerifyLinkUseCase {


    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final java.time.Clock clock;

    @Override
    @Transactional
    public VerifyLinkResult verifyLink(VerifyLinkCommand command) {
        Instant now = Instant.now(clock);

        String tokenHash = hasher.hash(command.token());

        var pendingOpt = repo.findActivePendingByTokenHash(
                command.purpose(),
                command.channel(),
                tokenHash
        );
        if (pendingOpt.isEmpty()) return new VerifyLinkResult(false, null, null);

        VerificationChallenge pending = pendingOpt.get();

        pending.expireIfNeeded(now);
        if (pending.getStatus() != VerificationStatus.PENDING) {
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        if (pending.getTokenExpiresAt() != null && now.isAfter(pending.getTokenExpiresAt())) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        if (pending.getAttemptCount() >= pending.getMaxAttempts()) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        pending.recordAttempt(now);

        boolean ok = tokenHash.equals(pending.getTokenHash());
        if (!ok) {
            repo.save(pending);
            return new VerifyLinkResult(false, null, null);
        }

        pending.markVerified(now);
        repo.save(pending);

        return new VerifyLinkResult(true, pending.getId(), pending.getDestinationNorm());
    }
}