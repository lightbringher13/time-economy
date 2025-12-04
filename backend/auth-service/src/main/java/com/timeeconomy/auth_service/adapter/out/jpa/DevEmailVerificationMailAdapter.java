// auth-service/src/main/java/com/timeeconomy/auth_service/adapter/out/mail/DevEmailVerificationMailAdapter.java
package com.timeeconomy.auth_service.adapter.out.jpa;

import com.timeeconomy.auth_service.domain.port.out.EmailVerificationMailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevEmailVerificationMailAdapter implements EmailVerificationMailPort {

    @Override
    public void sendVerificationCode(String email, String code) {
        // ⚠️ 진짜 이메일 발송 대신 로그 출력
        log.info("[DEV MAIL] Sending verification code. email={}, code={}", email, code);
    }
}