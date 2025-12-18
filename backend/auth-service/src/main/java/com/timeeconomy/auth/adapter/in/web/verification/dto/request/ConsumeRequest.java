package com.timeeconomy.auth.adapter.in.web.verification.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConsumeRequest(@NotNull String challengeId) {}
