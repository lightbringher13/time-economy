package com.timeeconomy.notification.adapter.out.brevo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.timeeconomy.notification.application.notification.exception.EmailSendFailedException;
import com.timeeconomy.notification.application.notification.port.out.EmailSenderPort;

class BrevoEmailSenderAdapterTest {

    private static final String BASE_URL = "http://brevo.test";
    private static final String CANARY_EMAIL = "provider-canary@example.test";
    private static final String CANARY_LINK =
            "https://app.example.test/verify?token=provider-secret-canary";

    private MockRestServiceServer server;
    private BrevoEmailSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        resetServer();
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void providerErrorBodyIsNotRetainedInTheRetryException() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"" + CANARY_EMAIL + " " + CANARY_LINK + "\"}"));

        EmailSendFailedException exception = assertThrows(
                EmailSendFailedException.class,
                () -> adapter.sendTemplate(linkCommand())
        );

        assertTrue(exception.getMessage().contains("status=400"));
        assertNull(exception.getCause());
        assertSecretSafe(exception);
    }

    @Test
    void transportErrorMessageAndCauseAreNotRetained() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(
                        new SocketTimeoutException("timeout " + CANARY_EMAIL + " " + CANARY_LINK)
                ));

        EmailSendFailedException exception = assertThrows(
                EmailSendFailedException.class,
                () -> adapter.sendTemplate(linkCommand())
        );

        assertTrue(exception.getMessage().contains("failureType=ResourceAccessException"));
        assertNull(exception.getCause());
        assertSecretSafe(exception);
    }

    private void resetServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrevoEmailSenderAdapter(builder.build());
        ReflectionTestUtils.setField(adapter, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(adapter, "createVerificationLinkTemplateId", 42);
    }

    private EmailSenderPort.EmailSendCommand linkCommand() {
        return new EmailSenderPort.EmailSendCommand(
                "LINK_EMAIL",
                CANARY_EMAIL,
                null,
                Map.of("linkUrl", CANARY_LINK)
        );
    }

    private static void assertSecretSafe(Throwable exception) {
        assertFalse(exception.getMessage().contains(CANARY_EMAIL));
        assertFalse(exception.getMessage().contains(CANARY_LINK));
        assertFalse(exception.toString().contains(CANARY_EMAIL));
        assertFalse(exception.toString().contains(CANARY_LINK));
    }
}
