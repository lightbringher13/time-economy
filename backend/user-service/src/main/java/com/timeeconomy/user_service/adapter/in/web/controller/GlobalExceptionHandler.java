package com.timeeconomy.user_service.adapter.in.web.controller;

import com.timeeconomy.user_service.adapter.in.web.dto.ErrorResponse;
import com.timeeconomy.user_service.domain.exception.InvalidUserProfileException;
import com.timeeconomy.user_service.domain.exception.UserProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidUserProfileException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidUserProfileException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_USER_PROFILE", ex.getMessage()));
    }
}