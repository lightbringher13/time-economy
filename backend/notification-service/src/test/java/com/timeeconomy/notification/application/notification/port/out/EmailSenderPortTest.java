package com.timeeconomy.notification.application.notification.port.out;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EmailSenderPortTest {

    private static final String CANARY_EMAIL = "command-canary@example.test";
    private static final String CANARY_NAME = "Command Canary Name";
    private static final String CANARY_OTP = "864209";
    private static final String CANARY_LINK = "https://example.test/verify?token=command-canary-secret";

    @Test
    void emailCommandToStringExposesOnlyTemplateKeyAndParameterKeys() {
        EmailSenderPort.EmailSendCommand command = new EmailSenderPort.EmailSendCommand(
                "LINK_EMAIL",
                CANARY_EMAIL,
                CANARY_NAME,
                Map.of(
                        "email", CANARY_EMAIL,
                        "otp", CANARY_OTP,
                        "linkUrl", CANARY_LINK
                )
        );

        String rendered = command.toString();

        assertTrue(rendered.contains("LINK_EMAIL"));
        assertTrue(rendered.contains("email"));
        assertTrue(rendered.contains("otp"));
        assertTrue(rendered.contains("linkUrl"));
        assertFalse(rendered.contains(CANARY_EMAIL));
        assertFalse(rendered.contains(CANARY_NAME));
        assertFalse(rendered.contains(CANARY_OTP));
        assertFalse(rendered.contains(CANARY_LINK));
    }
}
