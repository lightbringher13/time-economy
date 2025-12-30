package com.timeeconomy.notification.adapter.out.authclient;

import com.timeeconomy.notification.adapter.out.authclient.dto.OtpOnceResponse;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthInternalOtpRestClientAdapter implements AuthInternalOtpClientPort {

    @Qualifier("authInternalRestClient")
    private final RestClient authInternalRestClient;

    @Override
    public Optional<String> getOtpOnce(UUID verificationChallengeId) {
        try {
            OtpOnceResponse res = authInternalRestClient.get()
                    .uri("/internal/verification/challenges/{id}/otp", verificationChallengeId)
                    .retrieve()
                    .body(OtpOnceResponse.class);

            if (res == null || res.otp() == null || res.otp().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(res.otp());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return Optional.empty();
            throw e;
        } catch (ResourceAccessException e) {
            throw e;
        }
    }
}