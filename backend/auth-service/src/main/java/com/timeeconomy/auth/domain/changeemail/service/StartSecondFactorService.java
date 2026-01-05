package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth.domain.changeemail.port.in.StartSecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.changeemail.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class StartSecondFactorService implements StartSecondFactorUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final CreateOtpUseCase createOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public StartSecondFactorResult startSecondFactor(StartSecondFactorCommand command) {
        Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidSecondFactorCodeException();
        }

        // ✅ idempotent: already started or beyond
        if (request.getStatus() == EmailChangeStatus.SECOND_FACTOR_PENDING
                || request.getStatus() == EmailChangeStatus.READY_TO_COMMIT
                || request.getStatus() == EmailChangeStatus.COMPLETED) {
            return new StartSecondFactorResult(request.getId(), request.getSecondFactorType());
        }

        if (request.getStatus() != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
            throw new InvalidSecondFactorCodeException();
        }

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
        request.markSecondFactorPending(now); // you need this status+method
        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        return new StartSecondFactorResult(saved.getId(), type);
    }
}