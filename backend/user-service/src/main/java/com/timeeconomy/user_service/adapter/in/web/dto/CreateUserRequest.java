package com.timeeconomy.user_service.adapter.in.web.dto;

import java.time.LocalDate;

public record CreateUserRequest(
        Long id,
        String email,
        String name,
        String gender,
        LocalDate birthDate,
        String phoneNumber
) {}