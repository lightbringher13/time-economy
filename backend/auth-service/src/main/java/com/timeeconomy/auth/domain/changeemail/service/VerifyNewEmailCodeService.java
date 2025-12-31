package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth.domain.changeemail.port.in.VerifyNewEmailCodeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.exception.InvalidNewEmailCodeException;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import com.timeeconomy.auth.domain.verification.port.in.VerifyOtpUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class VerifyNewEmailCodeService implements VerifyNewEmailCodeUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;

    private final Clock clock;

    private final CreateOtpUseCase createOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;

    @Override
    @Transactional
    public VerifyNewEmailCodeResult verifyNewEmailCode(VerifyNewEmailCodeCommand command) {
        Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidNewEmailCodeException();
        }

        if (request.getStatus() != EmailChangeStatus.PENDING) {
            throw new InvalidNewEmailCodeException();
        }

        var verify = verifyOtpUseCase.verifyOtp(new VerifyOtpUseCase.VerifyOtpCommand(
                VerificationSubjectType.USER,
                command.userId().toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                request.getNewEmail(),
                command.code()
        ));

        if (!verify.success()) {
            throw new InvalidNewEmailCodeException();
        }

        request.markNewEmailVerified(now);

        AuthUser user = authUserRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

        SecondFactorType type;
        if (user.isPhoneVerified() && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            type = SecondFactorType.PHONE;

            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    command.userId().toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_PHONE,
                    VerificationChannel.SMS,
                    user.getPhoneNumber(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
        } else {
            type = SecondFactorType.OLD_EMAIL;

            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    command.userId().toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL,
                    VerificationChannel.EMAIL,
                    request.getOldEmail(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
        }

        request.setSecondFactorType(type, now);
        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        return new VerifyNewEmailCodeResult(saved.getId(), type);
    }
}