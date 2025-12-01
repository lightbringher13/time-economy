package com.timeeconomy.auth_service.adapter.in.web.dto;

public record ApiErrorResponse(
        String code,
        String message
) {}