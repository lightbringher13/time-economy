package com.timeeconomy.auth_service.adapter.out.mock;

import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationSmsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingSmsAdapter implements PhoneVerificationSmsPort {

    @Override
    public void sendVerificationCode(String countryCode, String phoneNumber, String code) {
        log.info("[SMS-MOCK] Would send verification code={} to {}{}", code, countryCode, phoneNumber);
    }
}