package com.timeeconomy.user.adapter.in.web.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.model.UserStatus;

public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        LocalDate birthDate,
        String gender,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
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