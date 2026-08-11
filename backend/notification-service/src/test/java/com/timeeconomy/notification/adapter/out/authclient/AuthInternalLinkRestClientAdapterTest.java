package com.timeeconomy.notification.adapter.out.authclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.LinkUrlPeekResponse;

class AuthInternalLinkRestClientAdapterTest {

    private static final String BASE_URL = "http://auth-service.test";
    private static final UUID CHALLENGE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_CHALLENGE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String PURPOSE = "SIGNUP_EMAIL";
    private static final String CANARY_LINK_URL =
            "https://app.example.test/verify?token=raw-secret-canary";
    private static final String LINK_PATH =
            "/internal/verification/challenges/" + CHALLENGE_ID + "/link-url";
    private static final String PEEK_URL = BASE_URL + LINK_PATH + "?purpose=" + PURPOSE;

    private MockRestServiceServer server;
    private AuthInternalLinkRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        resetServer();
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void peeksLinkUrlWithTheExistingPurposeAndEventHeaders() {
        expectPeek(withSuccess(validBody(), MediaType.APPLICATION_JSON));

        Optional<String> result = adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID);

        assertEquals(Optional.of(CANARY_LINK_URL), result);
    }

    @Test
    void mapsOnlyAnExplicitGetNotFoundToEmpty() {
        expectPeek(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID).isEmpty());
    }

    @Test
    void preservesForbiddenServerAndTimeoutFailures() {
        expectPeek(withStatus(HttpStatus.FORBIDDEN));
        assertThrows(
                HttpClientErrorException.Forbidden.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        server.verify();

        resetServer();
        expectPeek(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(
                HttpServerErrorException.InternalServerError.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        server.verify();

        resetServer();
        expectPeek(withException(new SocketTimeoutException("read timed out")));
        assertThrows(
                ResourceAccessException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
    }

    @Test
    void rejectsNullAndBlankSuccessfulBodiesAsProtocolViolations() {
        expectPeek(withSuccess("null", MediaType.APPLICATION_JSON));
        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        server.verify();

        resetServer();
        expectPeek(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
    }

    @Test
    void rejectsMalformedJsonWithoutRetainingTheDecodeCauseOrSecret() {
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","linkUrl":"%s" BROKEN
                """.formatted(CHALLENGE_ID, CANARY_LINK_URL),
                MediaType.APPLICATION_JSON
        ));

        AuthInternalLinkProtocolException exception = assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );

        assertFalse(exception.getMessage().contains(CANARY_LINK_URL));
        assertFalse(exception.toString().contains(CANARY_LINK_URL));
        assertNull(exception.getCause());
    }

    @Test
    void rejectsMissingMalformedAndMismatchedChallengeIds() {
        expectPeek(withSuccess(
                """
                {"linkUrl":"%s"}
                """.formatted(CANARY_LINK_URL),
                MediaType.APPLICATION_JSON
        ));
        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        server.verify();

        resetServer();
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"not-a-uuid","linkUrl":"%s"}
                """.formatted(CANARY_LINK_URL),
                MediaType.APPLICATION_JSON
        ));
        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        server.verify();

        resetServer();
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","linkUrl":"%s"}
                """.formatted(OTHER_CHALLENGE_ID, CANARY_LINK_URL),
                MediaType.APPLICATION_JSON
        ));
        AuthInternalLinkProtocolException mismatch = assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
        assertFalse(mismatch.getMessage().contains(CANARY_LINK_URL));
    }

    @Test
    void rejectsBlankLinkUrlAsAProtocolViolation() {
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","linkUrl":"  "}
                """.formatted(CHALLENGE_ID),
                MediaType.APPLICATION_JSON
        ));

        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.peekLinkUrl(CHALLENGE_ID, PURPOSE, EVENT_ID)
        );
    }

    @Test
    void acknowledgesLinkWithAnIdempotentDelete() {
        expectDelete(withStatus(HttpStatus.NO_CONTENT));

        adapter.acknowledgeLink(CHALLENGE_ID);
    }

    @Test
    void rejectsUnexpectedDeleteSuccessAndPreservesDeleteErrors() {
        expectDelete(withSuccess());
        assertThrows(
                AuthInternalLinkProtocolException.class,
                () -> adapter.acknowledgeLink(CHALLENGE_ID)
        );
        server.verify();

        resetServer();
        expectDelete(withStatus(HttpStatus.FORBIDDEN));
        assertThrows(
                HttpClientErrorException.Forbidden.class,
                () -> adapter.acknowledgeLink(CHALLENGE_ID)
        );
        server.verify();

        resetServer();
        expectDelete(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(
                HttpServerErrorException.InternalServerError.class,
                () -> adapter.acknowledgeLink(CHALLENGE_ID)
        );
    }

    @Test
    void responseAndProtocolExceptionNeverExposeTheRawLinkUrl() {
        LinkUrlPeekResponse response = new LinkUrlPeekResponse(CHALLENGE_ID.toString(), CANARY_LINK_URL);
        AuthInternalLinkProtocolException exception =
                new AuthInternalLinkProtocolException(CHALLENGE_ID, "linkUrl is invalid");

        assertFalse(response.toString().contains(CANARY_LINK_URL));
        assertTrue(response.toString().contains("linkUrl=<redacted>"));
        assertFalse(exception.getMessage().contains(CANARY_LINK_URL));
        assertFalse(exception.toString().contains(CANARY_LINK_URL));
        assertNull(exception.getCause());
    }

    private void expectPeek(org.springframework.test.web.client.ResponseCreator responseCreator) {
        server.expect(once(), requestTo(PEEK_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Event-Id", EVENT_ID.toString()))
                .andRespond(responseCreator);
    }

    private void expectDelete(org.springframework.test.web.client.ResponseCreator responseCreator) {
        server.expect(once(), requestTo(BASE_URL + LINK_PATH))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(responseCreator);
    }

    private void resetServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AuthInternalLinkRestClientAdapter(builder.build());
    }

    private String validBody() {
        return """
                {"verificationChallengeId":"%s","linkUrl":"%s"}
                """.formatted(CHALLENGE_ID, CANARY_LINK_URL);
    }
}
