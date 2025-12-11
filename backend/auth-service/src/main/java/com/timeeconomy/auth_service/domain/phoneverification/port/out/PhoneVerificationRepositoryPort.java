package com.timeeconomy.auth_service.domain.phoneverification.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

import com.timeeconomy.auth_service.domain.phoneverification.model.PhoneVerification;

public interface PhoneVerificationRepositoryPort {

    PhoneVerification save(PhoneVerification verification);

    /**
     * 해당 번호의 가장 최근 인증 시도(필요하면 만료 여부 포함).
     */
    Optional<PhoneVerification> findLatestByPhoneNumber(String phoneNumber);

    /**
     * 번호 + 코드로 조회.
     */
    Optional<PhoneVerification> findByPhoneAndCode(String phoneNumber, String code);

    /**
     * 아직 만료되지 않은 최신 인증 건.
     */
    default Optional<PhoneVerification> findLatestActiveByPhoneNumber(String phoneNumber, LocalDateTime now) {
        return findLatestByPhoneNumber(phoneNumber)
                .filter(v -> !v.isExpired(now));
    }
}