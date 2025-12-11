package com.timeeconomy.auth_service.adapter.in.web.changeemail.dto.response;

import com.timeeconomy.auth_service.domain.changeemail.model.SecondFactorType;

public record VerifyNewEmailCodeResponse(
            Long requestId,
            SecondFactorType secondFactorType
    ) {}

