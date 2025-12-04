package com.timeeconomy.auth_service.adapter.in.web.dto;

public record VerifyEmailCodeRequest(
        String email,
        String code) {
}