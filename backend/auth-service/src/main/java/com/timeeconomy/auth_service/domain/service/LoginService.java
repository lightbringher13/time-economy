package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.InvalidCredentialsException;
import com.timeeconomy.auth_service.domain.model.AuthSession;
import com.timeeconomy.auth_service.domain.port.in.LoginUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.JwtTokenPort;
import com.timeeconomy.auth_service.domain.port.out.RefreshTokenPort;
import com.timeeconomy.auth_service.domain.port.out.UserVerificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserVerificationPort userVerificationPort;
    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final JwtTokenPort jwtTokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    public LoginResult login(LoginCommand command) {
        // 1) Verify credentials via user-service (later)
        Long userId = userVerificationPort.verifyCredentials(
                        command.email(),
                        command.password()
                )
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2) Create refresh token + session
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String familyId = refreshTokenPort.generateFamilyId();
        String rawRefreshToken = refreshTokenPort.generateRefreshToken();
        String hashedRefreshToken = refreshTokenPort.hashRefreshToken(rawRefreshToken);

        LocalDateTime expiresAt = now.plusDays(7); // later: config

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

        // 3) Generate access token
        String accessToken = jwtTokenPort.generateAccessToken(userId, familyId);

        // 4) Return result (FE will get accessToken in JSON, refresh token via cookie)
        return new LoginResult(accessToken, rawRefreshToken);
    }
}