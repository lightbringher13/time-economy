package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.PhoneNumberAlreadyUsedException;
import com.timeeconomy.auth_service.domain.model.PhoneVerification;
import com.timeeconomy.auth_service.domain.port.in.RequestPhoneVerificationUseCase;
import com.timeeconomy.auth_service.domain.port.in.VerifyPhoneCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationRepositoryPort;
import com.timeeconomy.auth_service.domain.port.out.PhoneVerificationSmsPort;
import com.timeeconomy.auth_service.domain.port.out.SignupSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService
        implements RequestPhoneVerificationUseCase, VerifyPhoneCodeUseCase {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRES_MINUTES = 5;
    private static final String DEFAULT_COUNTRY_CODE = "+82";

    private final PhoneVerificationRepositoryPort phoneVerificationRepositoryPort;
    private final SignupSessionRepositoryPort signupSessionRepositoryPort;
    private final PhoneVerificationSmsPort phoneVerificationSmsPort;
    private final AuthUserRepositoryPort authUserRepositoryPort;
    // TODO: later inject SmsSenderPort or PhoneVerificationSmsPort for real SMS

    @Override
    public void requestVerification(RequestCommand command) {
        String phoneNumber = command.phoneNumber();
        String countryCode = command.countryCode() != null
                ? command.countryCode()
                : DEFAULT_COUNTRY_CODE;

        authUserRepositoryPort.findByPhoneNumber(phoneNumber)
                .ifPresent(existing -> {
                    throw new PhoneNumberAlreadyUsedException("Phone number is already in use");
                });
        
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

        // ⭐ send SMS using port
        phoneVerificationSmsPort.sendVerificationCode(countryCode, phoneNumber, code);

        log.info("[PHONE_VERIFICATION] code={} prepared for {}{}", code, countryCode, phoneNumber);
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

        if (success && command.signupSessionId() != null) {
            linkToSignupSession(command.signupSessionId(), phoneNumber, now);
        }

        return new Result(success);
    }

    private void linkToSignupSession(UUID signupSessionId, String phoneNumber, LocalDateTime now) {
        signupSessionRepositoryPort
                .findActiveById(signupSessionId, now)
                .ifPresent(session -> {
                    // 세션에 전화번호 반영 + 검증 표시
                    session.setPhoneNumber(phoneNumber);
                    session.setPhoneVerified(true);
                    session.setUpdatedAt(now); // 혹은 domain 메서드 하나 만들어도 됨

                    signupSessionRepositoryPort.save(session);

                    log.info("[PHONE_VERIFICATION] linked to signupSessionId={} phone={}",
                            session.getId(), phoneNumber);
                });
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