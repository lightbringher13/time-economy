package com.timeeconomy.auth_service.adapter.out.http.user;

import com.timeeconomy.auth_service.adapter.out.http.user.dto.CreateUserProfileRequest;
import com.timeeconomy.auth_service.domain.port.out.UserProfileSyncPort;
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
    public void createUserProfile(CreateUserProfileCommand command) {
        try {
            RestClient client = restClientBuilder.build();

            CreateUserProfileRequest body = new CreateUserProfileRequest(
                    command.authUserId(),
                    command.email(),
                    command.name(),
                    command.gender(),
                    command.birthDate(),
                    command.phoneNumber()
            );

            client.post()
                    .uri(userServiceBaseUrl + "/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Synced user profile to user-service: userId={}, email={}",
                    command.authUserId(), command.email());

        } catch (Exception ex) {
            log.error("Failed to sync user profile to user-service. userId={}, email={}",
                    command.authUserId(), command.email(), ex);
        }
    }
}