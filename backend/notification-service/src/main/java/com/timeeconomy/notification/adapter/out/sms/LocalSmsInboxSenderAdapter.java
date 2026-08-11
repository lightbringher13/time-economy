package com.timeeconomy.notification.adapter.out.sms;

import java.time.Clock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
@RequiredArgsConstructor
public class LocalSmsInboxSenderAdapter implements SmsSenderPort {

    private final LocalSmsInboxPort inbox;
    private final Clock clock;

    @Override
    public SmsSendResult sendTemplate(SmsSendCommand command) {
        LocalSmsInboxPort.Entry entry = new LocalSmsInboxPort.Entry(
                command.eventId(),
                command.challengeId(),
                command.otp(),
                maskDestination(command.toPhone()),
                command.templateKey(),
                command.purpose(),
                clock.instant(),
                command.expiresAt()
        );

        inbox.store(entry);
        log.info("[SMS][MOCK_INBOX] accepted eventId={} challengeId={} template={}",
                command.eventId(), command.challengeId(), command.templateKey());
        return new SmsSendResult("MOCK", "local-" + command.eventId());
    }

    private static String maskDestination(String destination) {
        if (destination.length() <= 4) {
            return "*".repeat(destination.length());
        }
        int visible = Math.min(4, destination.length());
        return "*".repeat(destination.length() - visible)
                + destination.substring(destination.length() - visible);
    }
}
