package com.timeeconomy.auth_service.domain.port.out;

public interface EmailNotificationPort {

    void sendSecurityAlert(Long userId, String subject, String message);
}