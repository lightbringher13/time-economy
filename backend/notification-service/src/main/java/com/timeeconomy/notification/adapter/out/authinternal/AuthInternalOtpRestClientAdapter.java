package com.timeeconomy.notification.adapter.out.authinternal;

import com.timeeconomy.notification.adapter.out.authinternal.dto.OtpOnceResponse;
import com.timeeconomy.notification.application.integration.port.out.AuthInternalOtpClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    private final RestClient restClient;

    @Value("${auth.internal.base-url}")
    private String baseUrl;

    @Value("${auth.internal.token}")
    private String internalToken;

    @Override
    public Optional<String> getOtpOnce(UUID verificationChallengeId) {
        String url = baseUrl + "/internal/verification/challenges/{id}/otp";

        try {
            OtpOnceResponse res = restClient.get()
                    .uri(url, verificationChallengeId.toString())
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(OtpOnceResponse.class);

            if (res == null || res.otp() == null || res.otp().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(res.otp());

        } catch (HttpClientErrorException e) {
            // 404 = already consumed/expired => non-retryable for your handler
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            // 401/403 and others -> retryable
            throw e;

        } catch (ResourceAccessException e) {
            // timeouts / connection issues -> retryable
            throw e;
        }
    }
}