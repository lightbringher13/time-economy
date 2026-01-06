package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

import java.time.Instant;

public record GetEmailChangeStatusResponse(
        Long requestId,
        EmailChangeStatus status,
        SecondFactorType secondFactorType,
        String maskedNewEmail,
        Instant expiresAt
) {}