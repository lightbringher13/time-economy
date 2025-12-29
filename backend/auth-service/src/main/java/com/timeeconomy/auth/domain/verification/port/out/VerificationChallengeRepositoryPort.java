package com.timeeconomy.auth.domain.verification.port.out;

import java.util.Optional;
import java.time.Duration;
import com.timeeconomy.auth.domain.verification.model.*;

public interface VerificationChallengeRepositoryPort {

    VerificationChallenge save(VerificationChallenge challenge);

    Optional<VerificationChallenge> findById(String id);

    void put(String challengeId, String rawCode, Duration ttl);

    Optional<String> getAndDelete(String challengeId);

    /**
     * 핵심 조회: subject+purpose+channel 에 대해 현재 활성 PENDING 1개
     * (DB에서 UNIQUE partial index로 보장하는 걸 추천)
     */
    Optional<VerificationChallenge> findActivePending(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel
    );

    /**
     * OTP 검증용 (PENDING + codeHash 일치)
     */
    Optional<VerificationChallenge> findPendingByCodeHash(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String destinationNorm,
            String codeHash
    );

    /**
     * LINK 검증용 (PENDING + tokenHash 일치)
     */
    Optional<VerificationChallenge> findPendingByTokenHash(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash
    );

    // ✅ NEW: public link verify (no subject info needed)
    Optional<VerificationChallenge> findActivePendingByTokenHash(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash
    );

    /**
     * status/polling: 목적(purpose) 기준 최신 요청
     */
    Optional<VerificationChallenge> findLatestByDestinationNormAndPurpose(
            String destinationNorm,
            VerificationPurpose purpose,
            VerificationChannel channel
    );
}