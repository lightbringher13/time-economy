package com.timeeconomy.auth_service.domain.emailverification.port.out;

public interface EmailVerificationMailPort {

    /**
     * 이메일 인증 코드 발송 (일반 인증용)
     */
    void sendVerificationCode(String email, String code);

    
}