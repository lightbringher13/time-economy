package com.timeeconomy.notification.adapter.out.sms;

import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("sms-mock & !prod")
public class LoggingSmsSenderAdapter implements SmsSenderPort {

    @Override
    public SmsSendResult sendTemplate(SmsSendCommand command) {
        log.info("[SMS][MOCK] accepted template={}", command.templateKey());
        return new SmsSendResult("MOCK", null);
    }
}
