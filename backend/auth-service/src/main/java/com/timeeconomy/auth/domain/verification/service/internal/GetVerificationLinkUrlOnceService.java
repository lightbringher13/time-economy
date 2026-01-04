package com.timeeconomy.auth.domain.verification.service.internal;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.verification.port.in.internal.GetVerificationLinkUrlOnceUseCase;
import com.timeeconomy.auth.domain.verification.port.out.VerificationChallengeRepositoryPort;
import com.timeeconomy.auth.domain.verification.port.out.internal.VerificationLinkBuilderPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetVerificationLinkUrlOnceService implements GetVerificationLinkUrlOnceUseCase {

    private final VerificationChallengeRepositoryPort repo;
    private final VerificationLinkBuilderPort linkBuilder;

    @Override
    public Result getOnce(Command command) {
        String rawToken = repo.getAndDeleteLinkToken(command.challengeId())
                .orElseThrow(() -> new NoSuchElementException("link token not found"));

        String linkUrl = linkBuilder.buildLinkUrl(rawToken, command.purpose());
        return new Result(command.challengeId(), linkUrl);
    }
}