package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;

public record VerifySecondFactorResponse(
        Long requestId,
        EmailChangeStatus status
) {}

