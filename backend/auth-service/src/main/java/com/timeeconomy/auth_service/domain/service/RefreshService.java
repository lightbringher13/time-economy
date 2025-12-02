package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.InvalidRefreshTokenException;
import com.timeeconomy.auth_service.domain.model.AuthSession;
import com.timeeconomy.auth_service.domain.port.in.RefreshUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.EmailNotificationPort;
import com.timeeconomy.auth_service.domain.port.out.JwtTokenPort;
import com.timeeconomy.auth_service.domain.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshService implements RefreshUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final RefreshTokenPort refreshTokenPort;
    private final JwtTokenPort jwtTokenPort;
    private final EmailNotificationPort emailNotificationPort;

    private static final long REFRESH_TTL_DAYS = 7L;
    private static final long BENIGN_RACE_WINDOW_SECONDS = 15L;

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshResult refresh(RefreshCommand command) {
        String rawRefreshToken = command.refreshToken();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }

        String tokenHash = refreshTokenPort.hashRefreshToken(rawRefreshToken);

        // 🔒 acquire DB row lock for this session
        AuthSession session = authSessionRepositoryPort.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        LocalDateTime now = LocalDateTime.now();

        // 1) already revoked → reuse path
        if (session.isRevoked()) {
            handleReuse(session, command, now);
        }

        // 2) expired → just mark & fail
        if (session.isExpired(now)) {
            session.revoke(now);
            authSessionRepositoryPort.save(session);
            throw new InvalidRefreshTokenException("Refresh token expired or revoked");
        }

        // 3) normal rotation
        session.revoke(now);
        authSessionRepositoryPort.save(session);

        String newRawRefresh = refreshTokenPort.generateRefreshToken();
        String newHash = refreshTokenPort.hashRefreshToken(newRawRefresh);

        LocalDateTime expiresAt = now.plusDays(REFRESH_TTL_DAYS);

        AuthSession newSession = new AuthSession(
                session.getUserId(),
                session.getFamilyId(),
                newHash,
                session.getDeviceInfo(),
                command.ipAddress(),
                command.userAgent(),
                now,
                expiresAt
        );

        newSession = authSessionRepositoryPort.save(newSession);

        String accessToken = jwtTokenPort.generateAccessToken(
                newSession.getUserId()
        );

        return new RefreshResult(
                newSession.getUserId(),
                accessToken,
                newRawRefresh,
                newSession.getFamilyId()
        );
    }

    private void handleReuse(AuthSession session, RefreshCommand command, LocalDateTime now) {
        if (isBenignRace(session, command, now)) {
            throw new InvalidRefreshTokenException("Refresh token already used");
        }

        if (!session.isReuseDetected()) {
            session.markReuseDetected();
            authSessionRepositoryPort.save(session);

            authSessionRepositoryPort.revokeFamily(session.getFamilyId(), now);

            emailNotificationPort.sendSecurityAlert(
                    session.getUserId(),
                    "[Security Alert] Refresh token reuse detected",
                    "We detected a suspicious reuse of your refresh token for device family: "
                            + session.getFamilyId()
                            + ". All related sessions have been revoked. Please sign in again."
            );
        }

        throw new InvalidRefreshTokenException("Refresh token reuse detected");
    }

    private boolean isBenignRace(AuthSession session, RefreshCommand command, LocalDateTime now) {
        LocalDateTime revokedAt = session.getRevokedAt();
        if (revokedAt == null) {
            return false;
        }

        long diffSeconds = Math.abs(Duration.between(revokedAt, now).getSeconds());
        if (diffSeconds > BENIGN_RACE_WINDOW_SECONDS) {
            return false;
        }

        boolean sameIp = Objects.equals(session.getIpAddress(), command.ipAddress());
        boolean sameUa = Objects.equals(session.getUserAgent(), command.userAgent());
        boolean sameDevice = Objects.equals(session.getDeviceInfo(), command.deviceInfo());

        return sameIp && sameUa && sameDevice;
    }
}