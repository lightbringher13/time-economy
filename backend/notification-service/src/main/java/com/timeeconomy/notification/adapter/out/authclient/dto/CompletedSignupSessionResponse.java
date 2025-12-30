package com.timeeconomy.notification.adapter.out.authclient.dto;

import java.time.LocalDate;

public record CompletedSignupSessionResponse(
        String email,
        String phoneNumber,
        String name,
        String gender,
        LocalDate birthDate,
        String state // "COMPLETED" expected
) {}