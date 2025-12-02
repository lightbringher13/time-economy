package com.timeeconomy.auth_service.adapter.out.http.user;

import com.timeeconomy.auth_service.domain.port.out.UserProfileSyncPort;
import com.timeeconomy.auth_service.adapter.out.http.dto.CreateUserProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceHttpAdapter implements UserProfileSyncPort {

    private final RestClient.Builder restClientBuilder;

    @Value("${userservice.base-url}")
    private String userServiceBaseUrl;

    @Override
    public void createUserProfile(Long userId, String email) {
        try {
            RestClient client = restClientBuilder.build();

            client.post()
                    .uri(userServiceBaseUrl + "/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateUserProfileRequest(userId, email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to sync user profile to user-service. userId={}, email={}",
                    userId, email, ex);
        }
    }
}