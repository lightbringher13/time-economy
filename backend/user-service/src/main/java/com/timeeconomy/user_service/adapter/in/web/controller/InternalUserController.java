package com.timeeconomy.user_service.adapter.in.web.controller;

import com.timeeconomy.user_service.domain.port.in.CreateUserProfileUseCase;
import com.timeeconomy.user_service.domain.port.in.GetUserProfileByIdUseCase;
import com.timeeconomy.user_service.adapter.in.web.dto.UserProfileResponse;
import com.timeeconomy.user_service.adapter.in.web.dto.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                        request.userId(),
                        request.email()
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