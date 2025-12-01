package com.timeeconomy.auth_service.adapter.out.email;

import com.timeeconomy.auth_service.domain.port.out.EmailNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailNotificationAdapter implements EmailNotificationPort {

    @Override
    public void sendSecurityAlert(Long userId, String subject, String message) {
        // TODO: 나중에 실제 이메일 서비스로 교체
        log.warn("SECURITY EMAIL to userId={} | subject={} | message={}", userId, subject, message);
    }
}