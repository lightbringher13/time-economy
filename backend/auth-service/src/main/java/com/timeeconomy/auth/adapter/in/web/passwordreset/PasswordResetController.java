package com.timeeconomy.auth.adapter.in.web.passwordreset;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.adapter.in.web.passwordreset.dto.request.ChangePasswordRequest;
import com.timeeconomy.auth.adapter.in.web.passwordreset.dto.request.PasswordResetRequest;
import com.timeeconomy.auth.adapter.in.web.passwordreset.dto.request.PerformPasswordResetRequest;
import com.timeeconomy.auth.domain.changepassword.port.in.ChangePasswordUseCase;
import com.timeeconomy.auth.domain.exception.AuthenticationRequiredException;
import com.timeeconomy.auth.domain.exception.WeakPasswordException;
import com.timeeconomy.auth.domain.passwordreset.port.in.RequestPasswordResetUseCase;
import com.timeeconomy.auth.domain.passwordreset.port.in.ResetPasswordUseCase;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    
    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestBody ChangePasswordRequest body
    ) {
        // 1) 인증 정보(X-User-Id) 없으면 에러
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new AuthenticationRequiredException("Missing authenticated user id");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new AuthenticationRequiredException("Invalid authenticated user id");
        }

        if (body.newPassword() == null || body.newPassword().isBlank()) {
            throw new WeakPasswordException("New password must not be empty");
        }

        // 3) 유스케이스 호출
        changePasswordUseCase.changePassword(
                new ChangePasswordUseCase.Command(
                        userId,
                        body.currentPassword(),
                        body.newPassword()
                )
        );

        // 4) 변경 성공 → 내용 없는 204 응답
        return ResponseEntity.noContent().build();
    }

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