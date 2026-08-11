package com.timeeconomy.notification.adapter.out.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;

@ExtendWith(OutputCaptureExtension.class)
class LocalSmsInboxSenderAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final String CANARY_PHONE = "+821012345678";
    private static final String CANARY_OTP = "731942";

    @Test
    void storesTheOtpWithoutLoggingSecrets(CapturedOutput output) {
        CapturingInbox inbox = new CapturingInbox();
        LocalSmsInboxSenderAdapter adapter = new LocalSmsInboxSenderAdapter(
                inbox,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        SmsSenderPort.SmsSendCommand command = new SmsSenderPort.SmsSendCommand(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "OTP_SMS",
                CANARY_PHONE,
                CANARY_OTP,
                "SIGNUP_PHONE",
                NOW.plusSeconds(300)
        );

        SmsSenderPort.SmsSendResult result = adapter.sendTemplate(command);

        LocalSmsInboxPort.Entry stored = inbox.entries.getFirst();
        assertEquals(CANARY_OTP, stored.otp());
        assertFalse(stored.maskedDestination().contains(CANARY_PHONE));
        assertEquals("5678", stored.maskedDestination().substring(stored.maskedDestination().length() - 4));
        assertEquals("MOCK", result.provider());
        assertNotNull(result.providerMsgId());
        assertFalse(output.getAll().contains(CANARY_PHONE));
        assertFalse(output.getAll().contains(CANARY_OTP));
        assertFalse(command.toString().contains(CANARY_PHONE));
        assertFalse(command.toString().contains(CANARY_OTP));
        assertFalse(stored.toString().contains(CANARY_OTP));
    }

    @Test
    void fullyMasksUnexpectedShortDestinations() {
        CapturingInbox inbox = new CapturingInbox();
        LocalSmsInboxSenderAdapter adapter = new LocalSmsInboxSenderAdapter(
                inbox,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        SmsSenderPort.SmsSendCommand command = new SmsSenderPort.SmsSendCommand(
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                UUID.fromString("20000000-0000-0000-0000-000000000004"),
                "OTP_SMS",
                "1234",
                CANARY_OTP,
                "SIGNUP_PHONE",
                NOW.plusSeconds(300)
        );

        adapter.sendTemplate(command);

        assertEquals("****", inbox.entries.getFirst().maskedDestination());
    }

    private static final class CapturingInbox implements LocalSmsInboxPort {
        private final List<Entry> entries = new ArrayList<>();

        @Override
        public void store(Entry entry) {
            entries.add(entry);
        }

        @Override
        public List<Entry> findRecent(int limit) {
            return List.copyOf(entries);
        }

        @Override
        public Optional<Entry> consume(UUID challengeId) {
            return Optional.empty();
        }
    }
}
