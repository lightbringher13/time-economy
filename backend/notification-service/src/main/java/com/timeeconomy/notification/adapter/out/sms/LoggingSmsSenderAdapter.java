package com.timeeconomy.notification.adapter.out.sms;

import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingSmsSenderAdapter implements SmsSenderPort {

    @Override
    public SmsSendResult sendTemplate(SmsSendCommand command) {
        log.info("[SMS][MOCK] template={} to={} vars={}",
                command.templateKey(), command.toPhone(), command.variables());
        return new SmsSendResult("LOG", null);
    }
}