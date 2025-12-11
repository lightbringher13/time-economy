package com.timeeconomy.auth_service.domain.phoneverification.model;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PhoneVerification {

    private final Long id;
    private final String phoneNumber;
    private final String countryCode;
    private final String code;
    private final LocalDateTime expiresAt;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime createdAt;

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public PhoneVerification markVerified(LocalDateTime now) {
        return PhoneVerification.builder()
                .id(this.id)
                .phoneNumber(this.phoneNumber)
                .countryCode(this.countryCode)
                .code(this.code)
                .expiresAt(this.expiresAt)
                .verifiedAt(now)
                .createdAt(this.createdAt)
                .build();
    }
}