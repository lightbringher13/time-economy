package com.timeeconomy.notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.timeeconomy.notification.adapter.in.web.LocalSmsInboxController;
import com.timeeconomy.notification.adapter.out.sms.InMemoryLocalSmsInboxAdapter;
import com.timeeconomy.notification.adapter.out.sms.LocalSmsInboxSenderAdapter;
import com.timeeconomy.notification.adapter.out.sms.LoggingSmsSenderAdapter;
import com.timeeconomy.notification.application.notification.port.in.LocalSmsInboxUseCase;
import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.application.notification.port.out.SmsSenderPort;
import com.timeeconomy.notification.application.notification.service.LocalSmsInboxService;

class LocalSmsInboxProfileTest {

    private static final String TOKEN_PROPERTY =
            "notification.sms.inbox.token=a-local-test-token-at-least-24-characters";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InboxConfiguration.class);

    @Test
    void inboxExistsOnlyInExplicitLocalMockEnvironments() {
        for (String profiles : List.of(
                "local,sms-mock,sms-inbox",
                "docker,sms-mock,sms-inbox",
                "test,sms-mock,sms-inbox"
        )) {
            contextRunner
                    .withPropertyValues("spring.profiles.active=" + profiles, TOKEN_PROPERTY)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        assertEquals(1, context.getBeansOfType(SmsSenderPort.class).size());
                        assertEquals(1, context.getBeansOfType(LocalSmsInboxPort.class).size());
                        assertEquals(1, context.getBeansOfType(LocalSmsInboxUseCase.class).size());
                        assertEquals(1, context.getBeansOfType(LocalSmsInboxController.class).size());
                        assertEquals(1, context.getBeansOfType(LocalSmsInboxTokenInterceptor.class).size());
                    });
        }
    }

    @Test
    void inboxBeansStayAbsentOutsideTheLocalSecurityBoundary() {
        for (String profiles : List.of(
                "sms-mock,sms-inbox",
                "docker,sms-inbox",
                "prod,sms-mock,sms-inbox",
                "prod,docker,sms-mock,sms-inbox"
        )) {
            contextRunner
                    .withPropertyValues("spring.profiles.active=" + profiles, TOKEN_PROPERTY)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        assertTrue(context.getBeansOfType(LocalSmsInboxPort.class).isEmpty());
                        assertTrue(context.getBeansOfType(LocalSmsInboxController.class).isEmpty());
                        assertTrue(context.getBeansOfType(LocalSmsInboxTokenInterceptor.class).isEmpty());
                    });
        }
    }

    @Test
    void ordinaryDockerMockUsesOnlyTheDiscardingSender() {
        contextRunner
                .withPropertyValues("spring.profiles.active=docker,sms-mock")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(SmsSenderPort.class).size());
                    assertEquals(1, context.getBeansOfType(LoggingSmsSenderAdapter.class).size());
                    assertTrue(context.getBeansOfType(LocalSmsInboxPort.class).isEmpty());
                });
    }

    @Test
    void enabledInboxFailsStartupWithoutAStrongToken() {
        contextRunner
                .withPropertyValues("spring.profiles.active=docker,sms-mock,sms-inbox")
                .run(context -> assertNotNull(context.getStartupFailure()));

        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=docker,sms-mock,sms-inbox",
                        "notification.sms.inbox.token=too-short"
                )
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void propertiesNeverPrintTheToken() {
        LocalSmsInboxProperties properties = new LocalSmsInboxProperties(
                "a-local-test-token-at-least-24-characters",
                100
        );

        assertFalse(properties.toString().contains(properties.token()));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            TimeConfig.class,
            LocalSmsInboxConfiguration.class,
            LoggingSmsSenderAdapter.class,
            InMemoryLocalSmsInboxAdapter.class,
            LocalSmsInboxSenderAdapter.class,
            LocalSmsInboxService.class,
            LocalSmsInboxController.class,
            LocalSmsInboxTokenInterceptor.class
    })
    static class InboxConfiguration {
    }
}
