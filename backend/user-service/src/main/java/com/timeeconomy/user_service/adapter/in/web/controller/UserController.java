package com.timeeconomy.user_service.adapter.in.web.controller;

import com.timeeconomy.user_service.adapter.in.web.dto.UserProfileResponse;
import com.timeeconomy.user_service.domain.exception.AuthenticationRequiredException;
import com.timeeconomy.user_service.domain.port.in.GetUserProfileByIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserProfileByIdUseCase getUserProfileByIdUseCase;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new AuthenticationRequiredException("Missing authenticated user id");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new AuthenticationRequiredException("Invalid authenticated user id");
        }

        var profile = getUserProfileByIdUseCase.getById(userId);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }
}