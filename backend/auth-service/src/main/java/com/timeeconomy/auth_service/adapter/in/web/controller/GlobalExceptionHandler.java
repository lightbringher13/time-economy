package com.timeeconomy.auth_service.adapter.in.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.timeeconomy.auth_service.adapter.in.web.dto.ApiErrorResponse;
import com.timeeconomy.auth_service.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;
import com.timeeconomy.auth_service.domain.exception.InvalidRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.SessionAlreadyRevokedException;

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

    @ExceptionHandler(MissingRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingToken(MissingRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiErrorResponse("MISSING_REFRESH_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(AuthSessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(AuthSessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiErrorResponse("AUTH_SESSION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SessionAlreadyRevokedException.class)
    public ResponseEntity<ApiErrorResponse> handleRevoked(SessionAlreadyRevokedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiErrorResponse("SESSION_ALREADY_REVOKED", ex.getMessage()));
    }
}