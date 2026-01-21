package com.timeeconomy.auth.adapter.in.web.signupsession.dto.request;

import java.time.LocalDate;

public record UpdateSignupProfileRequest(
        String name,
        String gender,         // "MALE" | "FEMALE" | "OTHER"
        LocalDate birthDate
) {}