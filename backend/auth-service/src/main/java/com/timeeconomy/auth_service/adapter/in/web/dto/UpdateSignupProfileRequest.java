package com.timeeconomy.auth_service.adapter.in.web.dto;

import java.time.LocalDate;

public record UpdateSignupProfileRequest(
        String name,
        String phoneNumber,
        String gender,     // "MALE" | "FEMALE" | "OTHER" or whatever you define
        LocalDate birthDate
) {}