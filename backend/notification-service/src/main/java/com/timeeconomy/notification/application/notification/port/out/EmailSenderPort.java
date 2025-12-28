// src/main/java/com/timeeconomy/notification/application/port/out/EmailSenderPort.java
package com.timeeconomy.notification.application.notification.port.out;

import java.util.Map;

public interface EmailSenderPort {

    EmailSendResult sendTemplate(EmailSendCommand command);

    record EmailSendCommand(
            String templateKey,          // e.g. "WELCOME_EMAIL"
            String toEmail,
            String toName,               // nullable
            Map<String, Object> params   // template variables
    ) {}

    record EmailSendResult(
            String provider,             // e.g. "BREVO"
            String providerMsgId         // nullable if provider doesn't return one
    ) {}
}