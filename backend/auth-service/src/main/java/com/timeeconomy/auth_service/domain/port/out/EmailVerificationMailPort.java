package com.timeeconomy.auth_service.domain.port.out;

public interface EmailVerificationMailPort {

    /**
     * 이메일 인증 코드 발송
     */
    void sendVerificationCode(String email, String code);
}