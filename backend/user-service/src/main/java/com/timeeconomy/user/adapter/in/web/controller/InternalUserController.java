package com.timeeconomy.user.adapter.in.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.user.adapter.in.web.dto.CreateUserRequest;
import com.timeeconomy.user.adapter.in.web.dto.UserProfileResponse;
import com.timeeconomy.user.domain.userprofile.port.in.CreateUserProfileUseCase;
import com.timeeconomy.user.domain.userprofile.port.in.GetUserProfileByIdUseCase;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final CreateUserProfileUseCase createUserProfileUseCase;
    private final GetUserProfileByIdUseCase getUserProfileByIdUseCase;

    // 회원가입 이후 auth-service가 호출할 API
    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest request) {
        createUserProfileUseCase.createProfile(
                new CreateUserProfileUseCase.Command(
                        request.id(),           // authUserId → user_profile.id
                        request.email(),
                        request.name(),
                        request.gender(),
                        request.birthDate(),
                        request.phoneNumber()
                )
        );
        return ResponseEntity.ok().build();
    }

    // 나중에 다른 서비스들이 userId로 프로필 조회할 때 쓰는 internal API
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUser(@PathVariable Long userId) {
        var profile = getUserProfileByIdUseCase.getById(userId);
        return ResponseEntity.ok(UserProfileResponse.from(profile));
    }
}