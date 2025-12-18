package com.timeeconomy.auth.domain.common.notification.port;

import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

public interface VerificationNotificationPort {

    /**
     * OTP(코드) 전송
     */
    void sendOtp(VerificationChannel channel, String destination, VerificationPurpose purpose, String code, int ttlMinutes);

    /**
     * 링크 전송 (비밀번호 재설정 등)
     */
    void sendLink(VerificationChannel channel, String destination, VerificationPurpose purpose, String linkUrl, int ttlMinutes);
}