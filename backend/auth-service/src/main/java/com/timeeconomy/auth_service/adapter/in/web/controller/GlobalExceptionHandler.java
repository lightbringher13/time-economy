package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.ApiErrorResponse;
import com.timeeconomy.auth_service.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;
import com.timeeconomy.auth_service.domain.exception.InvalidRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.SessionAlreadyRevokedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefresh(
            InvalidRefreshTokenException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_REFRESH_TOKEN",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MissingRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingToken(
            MissingRefreshTokenException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "MISSING_REFRESH_TOKEN",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AuthSessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotFound(
            AuthSessionNotFoundException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "AUTH_SESSION_NOT_FOUND",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(SessionAlreadyRevokedException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionRevoked(
            SessionAlreadyRevokedException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "SESSION_ALREADY_REVOKED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyUsed(
            EmailAlreadyUsedException ex,
            HttpServletRequest request
    ) {
        // 이미 사용 중인 이메일 → 409 CONFLICT 가 더 자연스러움
        HttpStatus status = HttpStatus.CONFLICT;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "EMAIL_ALREADY_USED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    // (선택) 예상 못한 예외 공통 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "AUTH_INTERNAL_ERROR",
                "Internal server error",
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }
}