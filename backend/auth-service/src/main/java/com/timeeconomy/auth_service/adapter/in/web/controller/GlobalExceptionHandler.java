package com.timeeconomy.auth_service.adapter.in.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.timeeconomy.auth_service.adapter.in.web.dto.ApiErrorResponse;
import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;
import com.timeeconomy.auth_service.domain.exception.InvalidRefreshTokenException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        var body = new ApiErrorResponse(
                "INVALID_CREDENTIALS",         // 🔹 stable code for FE to branch on
                ex.getMessage()                // 🔹 user-facing or dev-friendly text
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        var body = new ApiErrorResponse(
                "INVALID_REFRESH_TOKEN",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // You already have this one for reuse:
    // @ExceptionHandler(RefreshTokenReuseException.class) { ... }
}