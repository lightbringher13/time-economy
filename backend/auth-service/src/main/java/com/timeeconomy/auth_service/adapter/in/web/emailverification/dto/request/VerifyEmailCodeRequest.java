package com.timeeconomy.auth_service.adapter.in.web.emailverification.dto.request;

public record VerifyEmailCodeRequest(
        String email,
        String code) {
}