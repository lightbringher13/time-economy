package com.timeeconomy.auth.adapter.out.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth.adapter.out.notification.email.EmailSender;
import com.timeeconomy.auth.adapter.out.notification.sms.SmsSender;
import com.timeeconomy.auth.domain.common.notification.model.NotificationMessage;
import com.timeeconomy.auth.domain.common.notification.port.VerificationNotificationPort;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationNotificationAdapter implements VerificationNotificationPort {

    // ✅ adapter-internal helpers (NOT domain ports)
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    @Override
    public void sendOtp(
            VerificationChannel channel,
            String destination,
            VerificationPurpose purpose,
            String code,
            int ttlMinutes
    ) {
        NotificationMessage msg =
                NotificationMessage.otp(channel, destination, purpose, code, ttlMinutes);

        route(channel, msg);
    }

    @Override
    public void sendLink(
            VerificationChannel channel,
            String destination,
            VerificationPurpose purpose,
            String linkUrl,
            int ttlMinutes
    ) {
        // ✅ not dead: allow callers to use it, even if the actual sender templates are WIP
        NotificationMessage msg =
                NotificationMessage.link(channel, destination, purpose, linkUrl, ttlMinutes);

        // For now, you can still route and let sender decide how to handle LINK template,
        // OR you can keep it as a noop log.
        route(channel, msg);
    }

    private void route(VerificationChannel channel, NotificationMessage msg) {
        // ✅ exhaustive switch (no default) is better: compiler forces you to handle new channels later.
        switch (channel) {
            case EMAIL -> emailSender.send(msg);
            case SMS -> smsSender.send(msg);
        }
    }
}