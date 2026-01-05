package com.timeeconomy.auth.adapter.in.web.changeemail.dto.response;

import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;

public record StartSecondFactorResponse(Long requestId, SecondFactorType secondFactorType) {}
