package com.timeeconomy.user.domain.userprofile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.timeeconomy.user.domain.exception.InvalidUserProfileException;
import com.timeeconomy.user.domain.exception.UserProfileNotFoundException;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.model.UserStatus;
import com.timeeconomy.user.domain.userprofile.port.in.CreateUserProfileUseCase;
import com.timeeconomy.user.domain.userprofile.port.in.GetUserProfileByIdUseCase;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;

import java.time.Instant;

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

        Instant now = Instant.now();

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