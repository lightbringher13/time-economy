package com.timeeconomy.user.adapter.in.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.timeeconomy.user.adapter.in.web.dto.ApiErrorResponse;
import com.timeeconomy.user.domain.exception.AuthenticationRequiredException;
import com.timeeconomy.user.domain.exception.InvalidUserProfileException;
import com.timeeconomy.user.domain.exception.UserProfileNotFoundException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name:user-service}")
    private String serviceName;

    private ApiErrorResponse build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                false,                       // success
                serviceName,                 // service
                code,                        // code
                message,                     // message
                status.value(),              // status
                request.getRequestURI(),     // path
                Instant.now().toString()     // timestamp
        );
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            UserProfileNotFoundException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse body = build(
                status,
                "USER_NOT_FOUND",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidUserProfileException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalid(
            InvalidUserProfileException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = build(
                status,
                "INVALID_USER_PROFILE",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(
            AuthenticationRequiredException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ApiErrorResponse body = build(
                status,
                "AUTH_REQUIRED",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(status).body(body);
    }

    // (선택) 예측 못한 예외 공통 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = build(
                status,
                "USER_INTERNAL_ERROR",
                "Internal server error",
                request
        );
        return ResponseEntity.status(status).body(body);
    }
}