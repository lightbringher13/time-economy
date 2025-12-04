package com.timeeconomy.auth_service.domain.service;

import com.timeeconomy.auth_service.domain.exception.EmailVerificationAlreadyUsedException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationExpiredException;
import com.timeeconomy.auth_service.domain.exception.EmailVerificationNotFoundException;
import com.timeeconomy.auth_service.domain.model.EmailVerification;
import com.timeeconomy.auth_service.domain.port.in.VerifyEmailCodeUseCase;
import com.timeeconomy.auth_service.domain.port.out.EmailVerificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerifyEmailCodeService implements VerifyEmailCodeUseCase {

    private final EmailVerificationRepositoryPort emailVerificationRepositoryPort;

    @Override
    @Transactional
    public VerifyResult verify(VerifyCommand command) {
        String email = command.email();
        String code = command.code();

        LocalDateTime now = LocalDateTime.now();

        // 1) email + code 로 조회
        EmailVerification verification = emailVerificationRepositoryPort
                .findByEmailAndCode(email, code)
                .orElseThrow(() -> new EmailVerificationNotFoundException("Invalid verification code for email"));

        // 2) 만료 체크
        if (verification.isExpired(now)) {
            throw new EmailVerificationExpiredException("Verification code expired");
        }

        // 3) 이미 사용된 코드인지
        if (verification.isVerified()) {
            throw new EmailVerificationAlreadyUsedException("Verification code already used");
        }

        // 4) 정상: verifiedAt 업데이트
        verification.markVerified(now);
        emailVerificationRepositoryPort.save(verification);

        log.info("[EmailVerification] verified email={} code={}", email, code);

        return new VerifyResult(true);
    }
}