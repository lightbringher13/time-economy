package com.timeeconomy.auth_service.domain.port.out;

public interface PhoneVerificationSmsPort {

    void sendVerificationCode(String countryCode, String phoneNumber, String code);
}