package com.timeeconomy.auth_service.domain.port.out;

public interface EmailNotificationPort {

    void sendSecurityAlert(Long userId, String subject, String message);

    /**
     * 이메일 변경 완료 후, 기존 이메일 주소로 알림 발송
     */
    void notifyEmailChangedOldEmail(String oldEmail, String newEmail);

    /**
     * 이메일 변경 완료 후, 새 이메일 주소로 알림 발송
     */
    void notifyEmailChangedNewEmail(String newEmail);
}