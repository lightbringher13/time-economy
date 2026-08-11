package com.timeeconomy.notification.adapter.out.authclient;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.OtpPeekResponse;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AuthInternalOtpRestClientAdapter implements AuthInternalOtpClientPort {

    @Qualifier("authInternalRestClient")
    private final RestClient authInternalRestClient;

    @Override
    public Optional<String> peekOtp(UUID verificationChallengeId) {
        Objects.requireNonNull(verificationChallengeId, "verificationChallengeId");

        OtpPeekResponse response;
        try {
            var responseEntity = authInternalRestClient.get()
                    .uri("/internal/verification/challenges/{id}/otp", verificationChallengeId)
                    .retrieve()
                    .toEntity(OtpPeekResponse.class);

            if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                throw protocolException(
                        verificationChallengeId,
                        "expected HTTP 200 but received " + responseEntity.getStatusCode()
                );
            }

            response = responseEntity.getBody();
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw e;
        } catch (RestClientException e) {
            throw protocolException(verificationChallengeId, "response body could not be decoded");
        }

        validateResponse(verificationChallengeId, response);
        return Optional.of(response.otp());
    }

    @Override
    public void acknowledgeOtp(UUID verificationChallengeId) {
        Objects.requireNonNull(verificationChallengeId, "verificationChallengeId");

        var response = authInternalRestClient.delete()
                .uri("/internal/verification/challenges/{id}/otp", verificationChallengeId)
                .retrieve()
                .toBodilessEntity();

        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            throw protocolException(
                    verificationChallengeId,
                    "expected HTTP 204 from acknowledgement but received " + response.getStatusCode()
            );
        }
    }

    private static void validateResponse(UUID expectedChallengeId, OtpPeekResponse response) {
        if (response == null) {
            throw protocolException(expectedChallengeId, "response body is missing");
        }

        if (response.verificationChallengeId() == null
                || response.verificationChallengeId().isBlank()) {
            throw protocolException(expectedChallengeId, "verificationChallengeId is missing");
        }

        UUID actualChallengeId;
        try {
            actualChallengeId = UUID.fromString(response.verificationChallengeId());
        } catch (IllegalArgumentException invalidUuid) {
            throw protocolException(expectedChallengeId, "verificationChallengeId is malformed");
        }

        if (!expectedChallengeId.equals(actualChallengeId)) {
            throw protocolException(expectedChallengeId, "verificationChallengeId does not match the request");
        }

        if (response.otp() == null || response.otp().isBlank()) {
            throw protocolException(expectedChallengeId, "otp is missing");
        }
    }

    private static AuthInternalOtpProtocolException protocolException(UUID challengeId, String reason) {
        return new AuthInternalOtpProtocolException(challengeId, reason);
    }
}
