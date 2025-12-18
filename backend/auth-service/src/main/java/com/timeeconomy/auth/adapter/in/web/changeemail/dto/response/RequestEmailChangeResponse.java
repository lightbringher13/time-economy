package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

public record RequestEmailChangeResponse(
            Long requestId,
            String maskedNewEmail
    ) {}