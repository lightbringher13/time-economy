package com.timeeconomy.auth_service.domain.common.notification.port;

import java.util.Map;

public interface EmailNotificationPort {

    void sendSecurityAlert(Long userId, String alertKey, Map<String, Object> vars);

    void notifyEmailChangedOldEmail(String oldEmail, String newEmail);

    void notifyEmailChangedNewEmail(String newEmail);
}