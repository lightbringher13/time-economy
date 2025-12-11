package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.request;

public record RequestEmailChangeRequest(
            String currentPassword,
            String newEmail
    ) {}