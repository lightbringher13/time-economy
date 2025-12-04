package com.timeeconomy.user_service.adapter.in.web.dto;

import com.timeeconomy.user_service.domain.model.UserProfile;
import com.timeeconomy.user_service.domain.model.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        LocalDate birthDate,
        String gender,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getBirthDate(),
                profile.getGender(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}