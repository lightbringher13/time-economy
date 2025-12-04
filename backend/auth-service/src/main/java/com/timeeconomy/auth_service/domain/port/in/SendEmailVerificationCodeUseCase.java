package com.timeeconomy.auth_service.domain.port.in;

public interface SendEmailVerificationCodeUseCase {

    /**
     * 이메일로 인증 코드를 발송한다.
     */
    void send(SendCommand command);

    record SendCommand(
            String email,
            String ipAddress,
            String userAgent) {
    }
}