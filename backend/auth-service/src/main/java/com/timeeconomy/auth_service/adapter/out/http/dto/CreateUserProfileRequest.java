package com.timeeconomy.auth_service.adapter.out.http.dto;

import java.time.LocalDate;

public record CreateUserProfileRequest(
        Long id,
        String email,
        String name,
        String gender,
        LocalDate birthDate,
        String phoneNumber
) {}