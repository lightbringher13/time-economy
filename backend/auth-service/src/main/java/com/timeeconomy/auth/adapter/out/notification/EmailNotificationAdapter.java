package com.timeeconomy.auth.adapter.out.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.notification.email.EmailSender;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.common.notification.model.NotificationMessage;
import com.timeeconomy.auth.domain.common.notification.port.EmailNotificationPort;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements EmailNotificationPort {

    private final EmailSender emailSender;
    private final AuthUserRepositoryPort authUserRepositoryPort;

    @Override
    public void sendSecurityAlert(Long userId, String alertKey, Map<String, Object> vars) {
        String email = authUserRepositoryPort.findById(userId)
                .map(u -> u.getEmail())
                .orElse(null);

        if (email == null || email.isBlank()) {
            log.warn("[EmailNotification] cannot send security alert; email not found userId={}", userId);
            return;
        }

        NotificationMessage msg = NotificationMessage.securityAlert(email, alertKey, vars);
        emailSender.send(msg);
    }

    @Override
    public void notifyEmailChangedOldEmail(String oldEmail, String newEmail) {
        emailSender.send(NotificationMessage.emailChangedOld(oldEmail, newEmail));
    }

    @Override
    public void notifyEmailChangedNewEmail(String newEmail) {
        emailSender.send(NotificationMessage.emailChangedNew(newEmail));
    }
}