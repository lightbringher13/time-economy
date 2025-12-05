package com.timeeconomy.auth_service.domain.port.in;

public interface SendEmailVerificationCodeUseCase {

    /**
     * 이메일로 인증 코드를 발송하고, 생성된 코드를 반환한다 (dev 용도).
     */
    String send(SendCommand command);

    record SendCommand(
            String email,
            String ipAddress,
            String userAgent
    ) {}
}