package com.timeeconomy.auth.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationStatus;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.VerificationTokenHasherPort;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerifyOptService implements VerifyOtpUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationTokenHasherPort hasher;
    private final java.time.Clock clock;

    @Override
    @Transactional
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        Instant now = Instant.now(clock);

        var pendingOpt = repo.findActivePending(command.subjectType(), command.subjectId(), command.purpose(), command.channel());
        if (pendingOpt.isEmpty()) return new VerifyOtpResult(false);

        VerificationChallenge pending = pendingOpt.get();

        // bind destination
        String norm = normalizeDestination(command.channel(), command.destination());
        if (!pending.getDestinationNorm().equals(norm)) return new VerifyOtpResult(false);

        // expiry
        pending.expireIfNeeded(now);
        if (pending.getStatus() != VerificationStatus.PENDING) {
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        // attempt limit
        if (pending.getAttemptCount() >= pending.getMaxAttempts()) {
            pending.cancel(now);
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        // verify
        String codeHash = hasher.hash(command.code());
        pending.recordAttempt(now);

        boolean ok = codeHash.equals(pending.getCodeHash());
        if (!ok) {
            repo.save(pending);
            return new VerifyOtpResult(false);
        }

        pending.markVerified(now);
        repo.save(pending);
        return new VerifyOtpResult(true);
    }

    private String normalizeDestination(VerificationChannel channel, String destination) {
        if (destination == null) return "";
        String d = destination.trim();
        if (channel == VerificationChannel.EMAIL) return d.toLowerCase();
        // TODO: SMS normalize to E.164 later
        return d;
    }
}