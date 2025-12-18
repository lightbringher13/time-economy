package com.timeeconomy.auth.domain.passwordreset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.passwordreset.port.in.RequestPasswordResetUseCase;
import com.timeeconomy.auth.domain.verification.model.*;
import com.timeeconomy.auth.domain.verification.port.in.VerificationChallengeUseCase;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final VerificationChallengeUseCase verificationChallengeUseCase;

    @Value("${app.frontend.base-url:http://localhost:5173/reset-password}")
    private String linkBaseUrl; // e.g. https://fe.timeeconomy.com/reset-password

    @Override
    public Result requestReset(Command command) {
        String email = command.email().trim().toLowerCase();

        // BigCom: don't reveal user existence
        Optional<?> userOpt = authUserRepositoryPort.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("[PasswordReset] request for non-existing email={}", email);
            return new Result(true);
        }

        // subjectType/subjectId: for password reset, make it simple + stable
        // subjectType = EMAIL, subjectId = normalized email
        VerificationChallengeUseCase.CreateLinkCommand cmd =
                new VerificationChallengeUseCase.CreateLinkCommand(
                        VerificationSubjectType.EMAIL,
                        email,
                        VerificationPurpose.PASSWORD_RESET,
                        VerificationChannel.EMAIL,
                        email,
                        RESET_TTL,
                        RESET_TTL,
                        linkBaseUrl,
                        null,
                        null
                );

        verificationChallengeUseCase.createLink(cmd);
        return new Result(true);
    }
}