package com.timeeconomy.auth.adapter.in.web.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record RegisterRequest(
        String email,
        String password,
        String phoneNumber,
        String name,
        String gender,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate
) {}