package com.timeeconomy.user.adapter.in.web.dto;

public record UpdateUserProfileRequest(
        String name,
        String phoneNumber
) {}