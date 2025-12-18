package com.timeeconomy.auth.adapter.out.http.user.dto;

import java.time.LocalDate;

public record CreateUserProfileRequest(
        Long id,
        String email,
        String name,
        String gender,
        LocalDate birthDate,
        String phoneNumber
) {}