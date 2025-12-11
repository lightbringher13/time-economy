package com.timeeconomy.auth_service.domain.phoneverification.port.out;

public interface PhoneVerificationSmsPort {

    void sendVerificationCode(String countryCode, String phoneNumber, String code);
}