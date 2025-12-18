package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public record VerifyNewEmailCodeResponse(
            Long requestId,
            SecondFactorType secondFactorType
    ) {}

