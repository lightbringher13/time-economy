package com.timeeconomy.user_service.domain.service;

import com.timeeconomy.user_service.domain.exception.InvalidUserProfileException;
import com.timeeconomy.user_service.domain.exception.UserProfileNotFoundException;
import com.timeeconomy.user_service.domain.model.UserProfile;
import com.timeeconomy.user_service.domain.model.UserStatus;
import com.timeeconomy.user_service.domain.port.in.CreateUserProfileUseCase;
import com.timeeconomy.user_service.domain.port.in.GetUserProfileByIdUseCase;
import com.timeeconomy.user_service.domain.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileService implements
        CreateUserProfileUseCase,
        GetUserProfileByIdUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Override
    public void createProfile(Command command) {

        if (command.userId() == null) {
            throw new InvalidUserProfileException("UserId cannot be null");
        }
        if (command.email() == null || command.email().isBlank()) {
            throw new InvalidUserProfileException("Email cannot be null or empty");
        }

        // 멱등성 처리: 이미 있으면 뛰어넘기
        if (userProfileRepositoryPort.findById(command.userId()).isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserProfile profile = new UserProfile(
                command.userId(),
                command.email(),
                command.name(),
                command.phoneNumber(),
                UserStatus.ACTIVE,
                command.birthDate(),
                command.gender(),
                now,
                now
        );

        userProfileRepositoryPort.save(profile);
    }

    @Override
    public UserProfile getById(Long userId) {
        return userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));
    }
}