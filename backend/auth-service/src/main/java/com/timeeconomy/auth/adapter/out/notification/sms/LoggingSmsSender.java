package com.timeeconomy.auth.adapter.out.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.domain.common.notification.model.NotificationMessage;

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