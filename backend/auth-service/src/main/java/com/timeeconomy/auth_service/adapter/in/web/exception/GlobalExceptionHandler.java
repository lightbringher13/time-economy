package com.timeeconomy.auth_service.adapter.in.web.exception;

import com.timeeconomy.auth_service.adapter.in.web.exception.dto.response.ApiErrorResponse;
import com.timeeconomy.auth_service.domain.exception.AuthSessionNotFoundException;
import com.timeeconomy.auth_service.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth_service.domain.exception.AuthenticationRequiredException;
import com.timeeconomy.auth_service.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeEmailMismatchException;
import com.timeeconomy.auth_service.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;
import com.timeeconomy.auth_service.domain.exception.InvalidCurrentPasswordException;
import com.timeeconomy.auth_service.domain.exception.InvalidNewEmailCodeException;
import com.timeeconomy.auth_service.domain.exception.InvalidPasswordResetTokenException;
import com.timeeconomy.auth_service.domain.exception.InvalidRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth_service.domain.exception.MissingRefreshTokenException;
import com.timeeconomy.auth_service.domain.exception.PhoneNotVerifiedException;
import com.timeeconomy.auth_service.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.RefreshTokenReuseException;
import com.timeeconomy.auth_service.domain.exception.SessionAlreadyRevokedException;
import com.timeeconomy.auth_service.domain.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth_service.domain.exception.WeakPasswordException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationNotFoundException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationExpiredException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailNotVerifiedException;
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
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.UNAUTHORIZED;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "INVALID_CREDENTIALS",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidRefresh(
                        InvalidRefreshTokenException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.UNAUTHORIZED;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "INVALID_REFRESH_TOKEN",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(MissingRefreshTokenException.class)
        public ResponseEntity<ApiErrorResponse> handleMissingToken(
                        MissingRefreshTokenException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.BAD_REQUEST;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "MISSING_REFRESH_TOKEN",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(AuthSessionNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleSessionNotFound(
                        AuthSessionNotFoundException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.UNAUTHORIZED;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "AUTH_SESSION_NOT_FOUND",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(SessionAlreadyRevokedException.class)
        public ResponseEntity<ApiErrorResponse> handleSessionRevoked(
                        SessionAlreadyRevokedException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.UNAUTHORIZED;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "SESSION_ALREADY_REVOKED",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(EmailAlreadyUsedException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailAlreadyUsed(
                        EmailAlreadyUsedException ex,
                        HttpServletRequest request) {
                // 이미 사용 중인 이메일 → 409 CONFLICT 가 더 자연스러움
                HttpStatus status = HttpStatus.CONFLICT;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "EMAIL_ALREADY_USED",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        // (선택) 예상 못한 예외 공통 처리
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "AUTH_INTERNAL_ERROR",
                                "Internal server error",
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        // 🔹 이메일 인증 코드 없음
        @ExceptionHandler(EmailVerificationNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailVerificationNotFound(
                        EmailVerificationNotFoundException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.BAD_REQUEST;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "EMAIL_CODE_INVALID",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        // 🔹 이메일 인증 코드 만료
        @ExceptionHandler(EmailVerificationExpiredException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailVerificationExpired(
                        EmailVerificationExpiredException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.BAD_REQUEST;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "EMAIL_CODE_EXPIRED",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        // 🔹 이메일 인증 코드 이미 사용됨
        @ExceptionHandler(EmailVerificationAlreadyUsedException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailVerificationAlreadyUsed(
                        EmailVerificationAlreadyUsedException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.BAD_REQUEST;

                ApiErrorResponse body = new ApiErrorResponse(
                                false,
                                serviceName,
                                "EMAIL_CODE_USED",
                                ex.getMessage(),
                                status.value(),
                                request.getRequestURI(),
                                Instant.now().toString());

                return ResponseEntity.status(status).body(body);
        }

        // 🔹 Signup session not found or expired
        @ExceptionHandler(SignupSessionNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleSignupSessionNotFound(
                SignupSessionNotFoundException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // or 410 Gone if you prefer

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "SIGNUP_SESSION_NOT_FOUND",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        // 🔹 Email not verified when required
        @ExceptionHandler(EmailNotVerifiedException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailNotVerified(
                EmailNotVerifiedException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "EMAIL_NOT_VERIFIED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        // 🔹 Phone not verified when required
        @ExceptionHandler(PhoneNotVerifiedException.class)
        public ResponseEntity<ApiErrorResponse> handlePhoneNotVerified(
                PhoneNotVerifiedException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "PHONE_NOT_VERIFIED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(InvalidPasswordResetTokenException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidPasswordResetToken(
                InvalidPasswordResetTokenException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // 400 (same as others)

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_PASSWORD_RESET_TOKEN",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        // ===========================================
        // 🔥 AuthUserNotFoundException
        // ===========================================
        @ExceptionHandler(AuthUserNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleAuthUserNotFound(
                AuthUserNotFoundException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "AUTH_USER_NOT_FOUND",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        // ===========================================
        // 🔥 InvalidCurrentPasswordException
        // ===========================================
        @ExceptionHandler(InvalidCurrentPasswordException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidCurrentPassword(
                InvalidCurrentPasswordException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_CURRENT_PASSWORD",
                ex.getMessage(),   // "Current password is incorrect"
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        // ===========================================
        // 🔥 WeakPasswordException
        // ===========================================
        @ExceptionHandler(WeakPasswordException.class)
        public ResponseEntity<ApiErrorResponse> handleWeakPassword(
                WeakPasswordException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "WEAK_PASSWORD",
                ex.getMessage(),   // “Password must contain …”
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(AuthenticationRequiredException.class)
        public ResponseEntity<ApiErrorResponse> handleAuthenticationRequired(
                AuthenticationRequiredException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "AUTHENTICATION_REQUIRED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(PhoneNumberAlreadyUsedException.class)
        public ResponseEntity<ApiErrorResponse> handlePhoneNumberAlreadyUsed(
                PhoneNumberAlreadyUsedException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "PHONE_ALREADY_USED",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(RefreshTokenReuseException.class)
        public ResponseEntity<ApiErrorResponse> handleReuse(
                RefreshTokenReuseException ex,
                HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "REFRESH_TOKEN_REUSE",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(401).body(body);
        }


        @ExceptionHandler(EmailChangeRequestNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailChangeRequestNotFound(
                EmailChangeRequestNotFoundException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "EMAIL_CHANGE_REQUEST_NOT_FOUND",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(InvalidNewEmailCodeException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidNewEmailCode(
                InvalidNewEmailCodeException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_NEW_EMAIL_CODE",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(InvalidSecondFactorCodeException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidSecondFactorCode(
                InvalidSecondFactorCodeException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "INVALID_SECOND_FACTOR_CODE",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }

        @ExceptionHandler(EmailChangeEmailMismatchException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailChangeEmailMismatch(
                EmailChangeEmailMismatchException ex,
                HttpServletRequest request
        ) {
        HttpStatus status = HttpStatus.CONFLICT;

        ApiErrorResponse body = new ApiErrorResponse(
                false,
                serviceName,
                "EMAIL_CHANGE_MISMATCH",
                ex.getMessage(),
                status.value(),
                request.getRequestURI(),
                Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
        }
}