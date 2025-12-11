package com.timeeconomy.auth_service.adapter.out.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.timeeconomy.auth_service.domain.phoneverification.port.out.PhoneVerificationSmsPort;

@Component
@Slf4j
public class LoggingSmsAdapter implements PhoneVerificationSmsPort {

    @Override
    public void sendVerificationCode(String countryCode, String phoneNumber, String code) {
        log.info("[SMS-MOCK] Would send verification code={} to {}{}", code, countryCode, phoneNumber);
    }
}