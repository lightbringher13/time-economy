package com.timeeconomy.auth.domain.verification.port.in;

import java.time.Duration;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface CreateLinkUseCase {

    CreateLinkResult createLink(CreateLinkCommand command);

    record CreateLinkCommand(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,     // usually EMAIL
            String destination,              // email raw
            Duration ttl,                    // challenge TTL
            Duration tokenTtl,               // token TTL (often same as ttl)
            String requestIp,
            String userAgent
    ) {}

    record CreateLinkResult(
            String challengeId,
            boolean sent,
            int ttlMinutes,
            String maskedDestination
    ) {}
}