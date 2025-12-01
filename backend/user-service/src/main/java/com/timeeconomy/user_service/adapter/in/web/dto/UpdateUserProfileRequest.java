package com.timeeconomy.user_service.adapter.in.web.dto;

public record UpdateUserProfileRequest(
        String name,
        String phoneNumber
) {}