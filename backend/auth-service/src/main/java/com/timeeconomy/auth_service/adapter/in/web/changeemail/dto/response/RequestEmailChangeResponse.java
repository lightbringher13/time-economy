package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response;

public record RequestEmailChangeResponse(
            Long requestId,
            String maskedNewEmail
    ) {}