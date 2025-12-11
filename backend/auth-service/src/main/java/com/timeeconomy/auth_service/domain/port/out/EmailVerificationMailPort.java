package com.timeeconomy.auth_service.domain.port.out;

public interface EmailVerificationMailPort {

    /**
     * 이메일 인증 코드 발송 (일반 인증용)
     */
    void sendVerificationCode(String email, String code);

    /**
     * 이메일 변경 완료 후, 기존 이메일 주소로 알림 발송
     */
    void notifyEmailChangedOldEmail(String oldEmail, String newEmail);

    /**
     * 이메일 변경 완료 후, 새 이메일 주소로 알림 발송
     */
    void notifyEmailChangedNewEmail(String newEmail);
}