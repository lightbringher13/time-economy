package com.timeeconomy.notification.adapter.out.authclient;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.LinkUrlPeekResponse;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalLinkClientPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthInternalLinkRestClientAdapter implements AuthInternalLinkClientPort {

    private static final String LINK_URL_PATH = "/internal/verification/challenges/{id}/link-url";
    private static final String EVENT_ID_HEADER = "X-Event-Id";

    @Qualifier("authInternalRestClient")
    private final RestClient authInternalRestClient;

    @Override
    public Optional<String> peekLinkUrl(UUID challengeId, String purpose, UUID eventId) {
        Objects.requireNonNull(challengeId, "challengeId");
        Objects.requireNonNull(purpose, "purpose");

        LinkUrlPeekResponse response;
        try {
            var responseEntity = authInternalRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LINK_URL_PATH)
                            .queryParam("purpose", purpose)
                            .build(challengeId))
                    .header(EVENT_ID_HEADER, eventId == null ? "" : eventId.toString())
                    .retrieve()
                    .toEntity(LinkUrlPeekResponse.class);

            if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                throw protocolException(
                        challengeId,
                        "expected HTTP 200 but received " + responseEntity.getStatusCode()
                );
            }

            response = responseEntity.getBody();
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw e;
        } catch (RestClientException e) {
            throw protocolException(challengeId, "response body could not be decoded");
        }

        validateResponse(challengeId, response);
        return Optional.of(response.linkUrl());
    }

    @Override
    public void acknowledgeLink(UUID challengeId) {
        Objects.requireNonNull(challengeId, "challengeId");

        var response = authInternalRestClient.delete()
                .uri(LINK_URL_PATH, challengeId)
                .retrieve()
                .toBodilessEntity();

        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            throw protocolException(
                    challengeId,
                    "expected HTTP 204 from acknowledgement but received " + response.getStatusCode()
            );
        }
    }

    private static void validateResponse(UUID expectedChallengeId, LinkUrlPeekResponse response) {
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

        if (response.linkUrl() == null || response.linkUrl().isBlank()) {
            throw protocolException(expectedChallengeId, "linkUrl is missing");
        }
    }

    private static AuthInternalLinkProtocolException protocolException(UUID challengeId, String reason) {
        return new AuthInternalLinkProtocolException(challengeId, reason);
    }
}
