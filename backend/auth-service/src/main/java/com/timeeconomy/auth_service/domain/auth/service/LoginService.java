package com.timeeconomy.auth_service.domain.auth.service;

import com.timeeconomy.auth_service.domain.auth.model.AuthSession;
import com.timeeconomy.auth_service.domain.auth.model.AuthUser;
import com.timeeconomy.auth_service.domain.auth.port.in.LoginUseCase;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth_service.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.auth.port.out.JwtTokenPort;
import com.timeeconomy.auth_service.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth_service.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long REFRESH_TTL_DAYS = 7L;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final JwtTokenPort jwtTokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    public LoginResult login(LoginCommand command) {

        // 1) 이메일로 AuthUser 조회
        AuthUser user = authUserRepositoryPort.findByEmail(command.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 2) 계정 상태 체크 (LOCKED, DELETED, PENDING 등은 로그인 불가)
        if (!user.isActive()) {
            // 일부러 메시지는 모호하게 유지 (보안상)
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 3) 비밀번호 검증
        boolean passwordMatches = passwordEncoderPort.matches(
                command.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            // 실패 카운트 증가 & 잠금 처리
            user.markLoginFailure(now, MAX_FAILED_ATTEMPTS);
            authUserRepositoryPort.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 4) 로그인 성공 처리 (lastLoginAt, 실패 카운트 리셋 등)
        user.markLoginSuccess(now);
        authUserRepositoryPort.save(user);

        Long userId = user.getId(); // 🔑 이제 이게 canonical userId

        // 5) 새 refresh token + session 생성
        String familyId = refreshTokenPort.generateFamilyId();
        String rawRefreshToken = refreshTokenPort.generateRefreshToken();
        String hashedRefreshToken = refreshTokenPort.hashRefreshToken(rawRefreshToken);

        LocalDateTime expiresAt = now.plusDays(REFRESH_TTL_DAYS);

        AuthSession session = new AuthSession(
                userId,
                familyId,
                hashedRefreshToken,
                command.deviceInfo(),
                command.ipAddress(),
                command.userAgent(),
                now,
                expiresAt
        );

        authSessionRepositoryPort.save(session);

        // 6) access token 생성
        String accessToken = jwtTokenPort.generateAccessToken(userId);

        // 7) 결과 반환 (refresh는 쿠키로, access는 JSON)
        return new LoginResult(accessToken, rawRefreshToken);
    }
}