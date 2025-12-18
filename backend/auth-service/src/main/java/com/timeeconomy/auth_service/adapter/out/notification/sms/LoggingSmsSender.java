package com.timeeconomy.auth_service.adapter.out.notification.sms;

import com.timeeconomy.auth_service.domain.common.notification.model.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingSmsSender implements SmsSender {

    @Override
    public void send(NotificationMessage message) {
        log.info("[SmsSender] SMS to={} template={} vars={}",
                message.destination(),
                message.templateKey(),
                message.vars());
    }
}