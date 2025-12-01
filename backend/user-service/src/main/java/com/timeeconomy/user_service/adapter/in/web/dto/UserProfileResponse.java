package com.timeeconomy.user_service.adapter.in.web.dto;

import com.timeeconomy.user_service.domain.model.UserProfile;

public record UserProfileResponse(
        Long userId,
        String email,
        String name,
        String phoneNumber,
        String status,
        String createdAt,
        String updatedAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getStatus().name(),
                profile.getCreatedAt().toString(),
                profile.getUpdatedAt().toString()
        );
    }
}