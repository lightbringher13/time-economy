package com.timeeconomy.notification.adapter.out.authclient;

import com.timeeconomy.notification.adapter.out.authclient.dto.response.CompletedSignupSessionResponse;
import com.timeeconomy.notification.application.integration.port.out.SignupSessionInternalClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignupSessionInternalRestClientAdapter implements SignupSessionInternalClientPort {

    @Qualifier("authInternalRestClient")
    private final RestClient authInternalRestClient;

    @Override
    public CompletedSignupSessionResponse getCompletedSession(UUID signupSessionId) {
        try {
            return authInternalRestClient.get()
                    .uri("/internal/signup-sessions/{id}/completed", signupSessionId)
                    .retrieve()
                    .body(CompletedSignupSessionResponse.class);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalStateException("Signup session not completed yet: " + signupSessionId, e);
            }
            throw e;
        }
    }
}