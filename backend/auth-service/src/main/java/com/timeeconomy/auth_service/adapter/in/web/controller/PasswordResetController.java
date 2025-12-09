package com.timeeconomy.auth_service.adapter.in.web.controller;

import com.timeeconomy.auth_service.adapter.in.web.dto.PasswordResetRequest;
import com.timeeconomy.auth_service.adapter.in.web.dto.PerformPasswordResetRequest;
import com.timeeconomy.auth_service.domain.port.in.RequestPasswordResetUseCase;
import com.timeeconomy.auth_service.domain.port.in.ResetPasswordUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    /**
     * Forgot password entry point.
     *
     * Always returns 200 OK even if email does not exist,
     * to avoid leaking user existence.
     */
    @PostMapping("/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody PasswordResetRequest request) {
        var cmd = new RequestPasswordResetUseCase.Command(request.email());
        requestPasswordResetUseCase.requestReset(cmd);

        // FE message example: "If that email exists, we've sent reset instructions."
        return ResponseEntity.ok().build();
    }

    // PasswordResetController.java (예시)
    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestParam("token") String token,
            @RequestBody PerformPasswordResetRequest body
    ) {
        if (!body.newPassword().equals(body.confirmPassword())) {
            // 나중에 비즈니스 예외로 바꿔도 됨
            return ResponseEntity.badRequest().build();
        }

        resetPasswordUseCase.resetPassword(
                new ResetPasswordUseCase.Command(
                        token,
                        body.newPassword()
                )
        );

        return ResponseEntity.noContent().build(); // 204, body 없음
    }
}