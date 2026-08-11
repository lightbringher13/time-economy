package com.timeeconomy.notification.adapter.out.authclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
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

import com.timeeconomy.notification.adapter.out.authclient.dto.response.OtpPeekResponse;

class AuthInternalOtpRestClientAdapterTest {

    private static final String BASE_URL = "http://auth-service.test";
    private static final UUID CHALLENGE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_CHALLENGE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final String OTP = "731942";
    private static final String OTP_PATH =
            "/internal/verification/challenges/" + CHALLENGE_ID + "/otp";

    private MockRestServiceServer server;
    private AuthInternalOtpRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AuthInternalOtpRestClientAdapter(builder.build());
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void peeksOtpWithoutConsumingIt() {
        expectPeek(withSuccess(validBody(), MediaType.APPLICATION_JSON));

        Optional<String> result = adapter.peekOtp(CHALLENGE_ID);

        assertEquals(Optional.of(OTP), result);
    }

    @Test
    void mapsOnlyAnExplicitGetNotFoundToEmpty() {
        expectPeek(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.peekOtp(CHALLENGE_ID).isEmpty());
    }

    @Test
    void preservesForbiddenServerAndTimeoutFailures() {
        expectPeek(withStatus(HttpStatus.FORBIDDEN));
        assertThrows(HttpClientErrorException.Forbidden.class, () -> adapter.peekOtp(CHALLENGE_ID));
        server.verify();

        resetServer();
        expectPeek(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(HttpServerErrorException.InternalServerError.class, () -> adapter.peekOtp(CHALLENGE_ID));
        server.verify();

        resetServer();
        expectPeek(withException(new SocketTimeoutException("read timed out")));
        assertThrows(ResourceAccessException.class, () -> adapter.peekOtp(CHALLENGE_ID));
    }

    @Test
    void rejectsAnEmptySuccessfulResponseAsAProtocolViolation() {
        expectPeek(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(AuthInternalOtpProtocolException.class, () -> adapter.peekOtp(CHALLENGE_ID));
    }

    @Test
    void rejectsBlankOtpAsAProtocolViolation() {
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","otp":"  "}
                """.formatted(CHALLENGE_ID),
                MediaType.APPLICATION_JSON
        ));

        assertThrows(AuthInternalOtpProtocolException.class, () -> adapter.peekOtp(CHALLENGE_ID));
    }

    @Test
    void rejectsMalformedJsonAsAProtocolViolation() {
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","otp":"%s"
                """.formatted(CHALLENGE_ID, OTP),
                MediaType.APPLICATION_JSON
        ));

        AuthInternalOtpProtocolException exception = assertThrows(
                AuthInternalOtpProtocolException.class,
                () -> adapter.peekOtp(CHALLENGE_ID)
        );
        assertFalse(exception.getMessage().contains(OTP));
        assertFalse(exception.toString().contains(OTP));
        assertNull(exception.getCause());
    }

    @Test
    void rejectsAMismatchedChallengeIdAsAProtocolViolation() {
        expectPeek(withSuccess(
                """
                {"verificationChallengeId":"%s","otp":"%s"}
                """.formatted(OTHER_CHALLENGE_ID, OTP),
                MediaType.APPLICATION_JSON
        ));

        assertThrows(AuthInternalOtpProtocolException.class, () -> adapter.peekOtp(CHALLENGE_ID));
    }

    @Test
    void acknowledgesOtpWithAnIdempotentDelete() {
        server.expect(once(), requestTo(BASE_URL + OTP_PATH))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        adapter.acknowledgeOtp(CHALLENGE_ID);
    }

    @Test
    void rejectsANonNoContentAcknowledgementAndPreservesDeleteErrors() {
        server.expect(once(), requestTo(BASE_URL + OTP_PATH))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());
        assertThrows(AuthInternalOtpProtocolException.class, () -> adapter.acknowledgeOtp(CHALLENGE_ID));
        server.verify();

        resetServer();
        server.expect(once(), requestTo(BASE_URL + OTP_PATH))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThrows(HttpClientErrorException.Forbidden.class, () -> adapter.acknowledgeOtp(CHALLENGE_ID));
    }

    @Test
    void responseToStringNeverContainsTheOtp() {
        OtpPeekResponse response = new OtpPeekResponse(CHALLENGE_ID.toString(), OTP);

        assertFalse(response.toString().contains(OTP));
        assertTrue(response.toString().contains("otp=<redacted>"));
    }

    private void expectPeek(org.springframework.test.web.client.ResponseCreator responseCreator) {
        server.expect(once(), requestTo(BASE_URL + OTP_PATH))
                .andExpect(method(HttpMethod.GET))
                .andRespond(responseCreator);
    }

    private void resetServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AuthInternalOtpRestClientAdapter(builder.build());
    }

    private String validBody() {
        return """
                {"verificationChallengeId":"%s","otp":"%s"}
                """.formatted(CHALLENGE_ID, OTP);
    }
}
