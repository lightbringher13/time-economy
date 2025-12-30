package com.timeeconomy.auth.domain.changepassword.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changepassword.port.in.ChangePasswordUseCase;
import com.timeeconomy.auth.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.InvalidCurrentPasswordException;
import com.timeeconomy.auth.domain.exception.WeakPasswordException;

import java.time.Instant;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ChangePasswordService implements ChangePasswordUseCase {

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final Clock clock;

    @Override
    @Transactional
    public void changePassword(Command cmd) {
        Long authUserId = cmd.authUserId();
        String currentPassword = cmd.currentPassword();
        String newPassword = cmd.newPassword();

        // 1) 유저 조회
        AuthUser user = authUserRepositoryPort.findById(authUserId)
                .orElseThrow(() -> new AuthUserNotFoundException(authUserId));

        // 2) 현재 비밀번호 검증
        if (!passwordEncoderPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        // 3) 새 비밀번호 정책 검증 (심플 버전)
        validateNewPassword(newPassword);

        // 4) 새 비밀번호 해시 생성
        String newHash = passwordEncoderPort.encode(newPassword);

        // 5) 유저 도메인 업데이트
        Instant now = Instant.now(clock);
        user.setPasswordHash(newHash);
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        user.setUpdatedAt(now);

        // TODO: 필요하면 여기서 refresh token 전체 revoke 등 추가

        // 6) 저장
        authUserRepositoryPort.save(user);
    }

    /**
     * 아주 간단한 패스워드 정책 (나중에 PasswordPolicyPort로 분리 가능)
     */
    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters");
        }
        // 필요하면 추가 규칙:
        // - 숫자 포함
        // - 대소문자 조합
        // - 특수문자 포함
        // - 이전 비밀번호와 다르게 등등
    }
}