package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.model.PhoneVerification;
import com.timeeconomy.auth_service.domain.port.in.RequestPhoneVerificationUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyPhoneCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationUseCaseService
        implements RequestPhoneVerificationUseCase, VerifyPhoneCodeUseCase {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRES_MINUTES = 5;
    private static final String DEFAULT_COUNTRY_CODE = "+82";

    private final PhoneVerificationRepositoryPort phoneVerificationRepositoryPort;
    // TODO: later inject SmsSenderPort or PhoneVerificationSmsPort for real SMS

    @Override
    public void requestVerification(RequestCommand command) {
        String phoneNumber = command.phoneNumber();
        String countryCode = command.countryCode() != null
                ? command.countryCode()
                : DEFAULT_COUNTRY_CODE;

        String code = generateNumericCode(CODE_LENGTH);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRES_MINUTES);

        PhoneVerification verification = PhoneVerification.builder()
                .id(null)
                .phoneNumber(phoneNumber)
                .countryCode(countryCode)
                .code(code)
                .expiresAt(expiresAt)
                .createdAt(now)
                .verifiedAt(null)
                .build();

        phoneVerificationRepositoryPort.save(verification);

        // For now just log (mock SMS)
        log.info("[PHONE_VERIFICATION] Send code={} to {}{} (expires at {})",
                code, countryCode, phoneNumber, expiresAt);
    }

    @Override
    public Result verify(VerifyCommand command) {
        String phoneNumber = command.phoneNumber();
        String code = command.code();
        LocalDateTime now = LocalDateTime.now();

        boolean success = phoneVerificationRepositoryPort
                .findByPhoneAndCode(phoneNumber, code)
                .filter(v -> !v.isExpired(now))
                .map(v -> {
                    PhoneVerification updated = v.markVerified(now);
                    phoneVerificationRepositoryPort.save(updated);
                    return true;
                })
                .orElse(false);

        return new Result(success);
    }

    private String generateNumericCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 0~9
        }
        return sb.toString();
    }
}