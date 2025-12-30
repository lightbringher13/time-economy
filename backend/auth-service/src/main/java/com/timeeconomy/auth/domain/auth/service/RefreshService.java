package com.timeeconomy.auth.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timeeconomy.auth.domain.auth.model.AuthSession;
import com.timeeconomy.auth.domain.auth.port.in.RefreshUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.JwtTokenPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.common.notification.port.EmailNotificationPort;
import com.timeeconomy.auth.domain.exception.InvalidRefreshTokenException;
import com.timeeconomy.auth.domain.exception.RefreshTokenReuseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.time.Clock;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshService implements RefreshUseCase {

    private final AuthSessionRepositoryPort authSessionRepositoryPort;
    private final RefreshTokenPort refreshTokenPort;
    private final JwtTokenPort jwtTokenPort;
    private final EmailNotificationPort emailNotificationPort;

    private final Clock clock;

    private static final Duration REFRESH_TTL_DAYS = Duration.ofDays(7);
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

        Instant now = Instant.now(clock);

        // 1) already revoked → reuse path
        if (session.isRevoked()) {
            RefreshResult reuseResult = handleReuse(session, command, now);
            if (reuseResult != null) {
                return reuseResult; // ⭐ benign case returns tokens
            }
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

        Instant expiresAt = now.plus(REFRESH_TTL_DAYS);

        AuthSession newSession = new AuthSession(
                session.getUserId(),
                session.getFamilyId(),
                newHash,
                session.getDeviceInfo(),
                command.ipAddress(),
                command.userAgent(),
                now,
                expiresAt);

        newSession = authSessionRepositoryPort.save(newSession);

        String accessToken = jwtTokenPort.generateAccessToken(
                newSession.getUserId());

        return new RefreshResult(
                newSession.getUserId(),
                accessToken,
                newRawRefresh,
                newSession.getFamilyId());
    }

    private RefreshResult handleReuse(AuthSession session, RefreshCommand command, Instant now) {
        // ⭐ BENIGN CASE — normal behavior
        if (isBenignRace(session, command, now)) {

            // The benign case means another parallel request ALREADY created new session.
            // So we must find the latest active session for this device family.

            AuthSession latest = authSessionRepositoryPort
                    .findLatestActiveByFamily(session.getFamilyId(), now)
                    .orElseThrow(() -> new InvalidRefreshTokenException("No active session found"));

            // Generate fresh access token for the new session
            String accessToken = jwtTokenPort.generateAccessToken(latest.getUserId());

            // ⭐ Return a normal refresh result instead of throwing
            return new RefreshResult(
                    latest.getUserId(),
                    accessToken,
                    null, // no new refresh token (cookie is already set)
                    latest.getFamilyId());
        }

        if (!session.isReuseDetected()) {
            session.markReuseDetected();
            authSessionRepositoryPort.save(session);

            authSessionRepositoryPort.revokeFamily(session.getFamilyId(), now);

            emailNotificationPort.sendSecurityAlert(
                    session.getUserId(),
                    "SECURITY_REFRESH_TOKEN_REUSE",
                    java.util.Map.of(
                            "familyId", session.getFamilyId(),
                            "ipAddress", command.ipAddress(),
                            "userAgent", command.userAgent(),
                            "deviceInfo", command.deviceInfo()
                    )
            );
        }

        throw new RefreshTokenReuseException("Refresh token reuse detected");
    }

    private boolean isBenignRace(AuthSession session, RefreshCommand command, Instant now) {
        Instant revokedAt = session.getRevokedAt();
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