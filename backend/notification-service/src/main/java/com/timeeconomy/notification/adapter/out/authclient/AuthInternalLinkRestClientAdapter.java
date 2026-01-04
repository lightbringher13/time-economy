package com.timeeconomy.notification.adapter.out.authclient;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.LinkUrlOnceResponse;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalLinkClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInternalLinkRestClientAdapter implements AuthInternalLinkClientPort {

    @Qualifier("authInternalRestClient")
    private final RestClient authInternalRestClient;

    @Value("${app.internal.token}")
    private String internalToken;

    @Override
    public Optional<String> getLinkUrlOnce(UUID verificationChallengeId, String purpose, UUID eventId) {
        try {
            LinkUrlOnceResponse res = authInternalRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/verification/challenges/{id}/link-url")
                            .queryParam("purpose", purpose)
                            .build(verificationChallengeId))
                    .header("X-Internal-Token", internalToken)
                    .header("X-Event-Id", eventId == null ? "" : eventId.toString())
                    .retrieve()
                    .body(LinkUrlOnceResponse.class);

            if (res == null || res.linkUrl() == null || res.linkUrl().isBlank()) {
                log.warn("[AUTH-INTERNAL] link-url empty response challengeId={} purpose={} eventId={}",
                        verificationChallengeId, purpose, eventId);
                return Optional.empty();
            }

            return Optional.of(res.linkUrl());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("[AUTH-INTERNAL] link-url NOT_FOUND challengeId={} purpose={} eventId={}",
                        verificationChallengeId, purpose, eventId);
                return Optional.empty();
            }

            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                // This is config/security bug — better to fail loudly
                log.error("[AUTH-INTERNAL] link-url FORBIDDEN (check X-Internal-Token) challengeId={} purpose={} eventId={}",
                        verificationChallengeId, purpose, eventId);
                throw e;
            }

            throw e;

        } catch (ResourceAccessException e) {
            // network / DNS / timeout -> retryable depending on your Kafka error strategy
            log.error("[AUTH-INTERNAL] link-url resource access error challengeId={} purpose={} eventId={}",
                    verificationChallengeId, purpose, eventId, e);
            throw e;
        }
    }
}