package com.timeeconomy.notification.adapter.out.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;

@ExtendWith(OutputCaptureExtension.class)
class LoggingSmsSenderAdapterTest {

    private static final String CANARY_PHONE = "+821012345678";
    private static final String CANARY_OTP = "731942";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SmsSenderRequiredConfiguration.class);

    @Test
    void mockSmsSenderIsAvailableOnlyWhenExplicitlyEnabled() {
        for (String profiles : List.of(
                "sms-mock",
                "test,sms-mock",
                "docker,sms-mock"
        )) {
            contextRunner
                    .withPropertyValues("spring.profiles.active=" + profiles)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        assertEquals(1, context.getBeansOfType(SmsSenderPort.class).size());
                    });
        }
    }

    @Test
    void applicationFailsClosedWhenMockSmsIsNotAllowed() {
        contextRunner.run(context -> assertMissingSmsSender(context.getStartupFailure()));

        for (String profiles : List.of(
                "docker",
                "prod",
                "prod,sms-mock"
        )) {
            contextRunner
                    .withPropertyValues("spring.profiles.active=" + profiles)
                    .run(context -> assertMissingSmsSender(context.getStartupFailure()));
        }
    }

    @Test
    void mockSmsSenderDoesNotExposeOtpOrPhone(CapturedOutput output) {
        SmsSenderPort.SmsSendCommand command = new SmsSenderPort.SmsSendCommand(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "OTP_SMS",
                CANARY_PHONE,
                CANARY_OTP,
                "SIGNUP_PHONE",
                Instant.parse("2026-08-09T12:05:00Z")
        );

        SmsSenderPort.SmsSendResult result = new LoggingSmsSenderAdapter()
                .sendTemplate(command);

        assertEquals("MOCK", result.provider());
        assertFalse(output.getAll().contains(CANARY_PHONE));
        assertFalse(output.getAll().contains(CANARY_OTP));
        assertFalse(command.toString().contains(CANARY_PHONE));
        assertFalse(command.toString().contains(CANARY_OTP));
    }

    private void assertMissingSmsSender(Throwable startupFailure) {
        assertNotNull(startupFailure);

        Throwable current = startupFailure;
        while (current != null && !(current instanceof NoSuchBeanDefinitionException)) {
            current = current.getCause();
        }

        assertTrue(current instanceof NoSuchBeanDefinitionException);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(LoggingSmsSenderAdapter.class)
    static class SmsSenderRequiredConfiguration {

        @Bean
        Object smsSenderRequired(SmsSenderPort smsSenderPort) {
            return new Object();
        }
    }
}
