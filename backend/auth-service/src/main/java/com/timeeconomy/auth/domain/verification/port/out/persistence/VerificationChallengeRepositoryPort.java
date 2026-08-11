package com.timeeconomy.auth.domain.verification.port.out.persistence;

import java.time.Duration;
import java.util.Optional;

import com.timeeconomy.auth.domain.verification.model.VerificationChallenge;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;

public interface VerificationChallengeRepositoryPort {

    // -------------------------
    // Core persistence
    // -------------------------

    VerificationChallenge save(VerificationChallenge challenge);

    Optional<VerificationChallenge> findById(String id);

    // -------------------------
    // Core lookup (1 active pending)
    // -------------------------

    /**
     * 핵심 조회: subject+purpose+channel 에 대해 현재 활성 PENDING 1개
     * (DB라면 UNIQUE partial index로 강제 추천)
     */
    Optional<VerificationChallenge> findActivePending(
            VerificationSubjectType subjectType,
            String subjectId,
            VerificationPurpose purpose,
            VerificationChannel channel
    );

    /**
     * ✅ Public link verify (subject info 없이 tokenHash만으로 찾는 케이스)
     * - password reset / magic link 등에 유용
     */
    Optional<VerificationChallenge> findActivePendingByTokenHash(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String tokenHash
    );

    /**
     * (Optional) status/polling: destinationNorm+purpose+channel 기준 최신 요청
     * - UI에서 "가장 최근에 보낸 OTP가 언제 만료되는지" 같은 조회에 활용 가능
     */
    Optional<VerificationChallenge> findLatestByDestinationNormAndPurpose(
            String destinationNorm,
            VerificationPurpose purpose,
            VerificationChannel channel
    );

    // -------------------------
    // Raw secret store (OTP / LINK token)
    // -------------------------

    /**
     * OTP raw code 저장 (Redis 등). TTL은 challenge.expiresAt 기반과 동일하게 맞추는 게 이상적.
     */
    void putOtp(String challengeId, String rawCode, Duration ttl);

    /**
     * OTP raw code를 전달용으로 조회한다. 조회만으로 TTL을 연장하거나 값을 삭제하지 않는다.
     */
    Optional<String> peekOtp(String challengeId);

    /**
     * 전달이 끝난 OTP raw code를 삭제한다. 이미 없더라도 안전한 멱등 연산이다.
     */
    void deleteOtp(String challengeId);

    /**
     * Link raw token 저장
     */
    void putLinkToken(String challengeId, String rawToken, Duration ttl);

    /**
     * Link raw token 1회성 읽기
     */
    Optional<String> getAndDeleteLinkToken(String challengeId);
}
