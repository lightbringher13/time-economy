package com.timeeconomy.auth_service.domain.auth.port.in;

public interface RefreshUseCase {

    RefreshResult refresh(RefreshCommand command);

    /**
     * What the refresh endpoint receives.
     * (We read refreshToken from cookie, IP / UA from request)
     */
    record RefreshCommand(
            String refreshToken,
            String ipAddress,
            String userAgent,
            String deviceInfo  // optional; you can pass null for now
    ) {}

    /**
     * What the domain returns to the controller.
     * FE only really needs accessToken, but we keep refreshToken
     * so the controller can set a new cookie.
     */
    record RefreshResult(
            Long userId,
            String accessToken,
            String refreshToken,
            String familyId
    ) {}
}