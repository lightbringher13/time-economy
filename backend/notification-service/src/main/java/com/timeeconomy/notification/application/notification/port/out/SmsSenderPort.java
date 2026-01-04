package com.timeeconomy.notification.application.notification.port.out;

import java.util.Map;

public interface SmsSenderPort {

    record SmsSendCommand(
            String templateKey,
            String toPhone,
            Map<String, Object> variables
    ) {}

    record SmsSendResult(
            String provider,
            String providerMsgId
    ) {}

    SmsSendResult sendTemplate(SmsSendCommand command);
}