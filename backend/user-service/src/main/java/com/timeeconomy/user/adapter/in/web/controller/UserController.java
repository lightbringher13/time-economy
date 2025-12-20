package com.timeeconomy.user.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.user.adapter.in.web.dto.UserProfileResponse;
import com.timeeconomy.user.domain.exception.AuthenticationRequiredException;
import com.timeeconomy.user.domain.userprofile.port.in.GetUserProfileByIdUseCase;

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