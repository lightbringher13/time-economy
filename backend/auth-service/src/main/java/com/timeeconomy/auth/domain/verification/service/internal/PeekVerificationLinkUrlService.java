package com.timeeconomy.auth.domain.verification.service.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationLinkUrlUseCase;
import com.timeeconomy.auth.domain.verification.port.out.internal.VerificationLinkBuilderPort;
import com.timeeconomy.auth.domain.verification.port.out.persistence.VerificationChallengeRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PeekVerificationLinkUrlService implements PeekVerificationLinkUrlUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationLinkBuilderPort linkBuilder;
    private final Clock clock;

    @Override
    public Result peek(Command command) {
        String challengeId = command == null ? null : command.challengeId();
        if (challengeId == null || challengeId.isBlank() || command.purpose() == null) {
            throw notFound();
        }

        Instant now = Instant.now(clock);
        VerificationChallenge challenge = repo.findById(challengeId)
                .filter(VerificationChallenge::isLink)
                .filter(VerificationChallenge::isPending)
                .filter(candidate -> !candidate.isExpired(now))
                .filter(candidate -> candidate.getChannel() == VerificationChannel.EMAIL)
                .filter(candidate -> candidate.getPurpose() == command.purpose())
                .orElseThrow(PeekVerificationLinkUrlService::notFound);

        String rawToken = repo.peekLinkToken(challenge.getId())
                .orElseThrow(PeekVerificationLinkUrlService::notFound);

        String linkUrl = linkBuilder.buildLinkUrl(rawToken, challenge.getPurpose());
        return new Result(challenge.getId(), linkUrl);
    }

    private static NoSuchElementException notFound() {
        return new NoSuchElementException("link URL not found");
    }
}
