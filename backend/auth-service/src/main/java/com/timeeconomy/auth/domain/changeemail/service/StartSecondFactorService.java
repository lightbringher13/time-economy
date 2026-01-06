package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import com.timeeconomy.auth.domain.changeemail.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth.domain.changeemail.port.in.StartSecondFactorUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.model.VerificationSubjectType;
import com.timeeconomy.auth.domain.verification.port.in.CreateOtpUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
        final Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        if (request.isExpired(now)) {
            request.markExpired(now);
            emailChangeRequestRepositoryPort.save(request);
            throw new InvalidSecondFactorCodeException();
        }

        if (request.getStatus() == EmailChangeStatus.CANCELED
                || request.getStatus() == EmailChangeStatus.EXPIRED) {
            throw new InvalidSecondFactorCodeException();
        }

        // idempotent cases
        if (request.getStatus() == EmailChangeStatus.SECOND_FACTOR_PENDING
                || request.getStatus() == EmailChangeStatus.READY_TO_COMMIT
                || request.getStatus() == EmailChangeStatus.COMPLETED) {

            SecondFactorType existingType = request.getSecondFactorType();
            if (existingType == null) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        request.getStatus() + " but secondFactorType is null"
                );
            }
            return new StartSecondFactorResult(request.getId(), existingType, request.getStatus());
        }

        if (request.getStatus() != EmailChangeStatus.NEW_EMAIL_VERIFIED) {
            throw new InvalidSecondFactorCodeException();
        }

        AuthUser user = authUserRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

        // decide type + destination
        final SecondFactorType type;
        final VerificationChannel channel;
        final VerificationPurpose purpose;
        final String destination;

        if (user.isPhoneVerified()
                && user.getPhoneNumber() != null
                && !user.getPhoneNumber().isBlank()) {

            type = SecondFactorType.PHONE;
            channel = VerificationChannel.SMS;
            purpose = VerificationPurpose.CHANGE_EMAIL_2FA_PHONE;
            destination = user.getPhoneNumber();

        } else {
            String oldEmail = request.getOldEmail();
            if (oldEmail == null || oldEmail.isBlank()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "Cannot start OLD_EMAIL second factor because request.oldEmail is missing"
                );
            }

            type = SecondFactorType.OLD_EMAIL;
            channel = VerificationChannel.EMAIL;
            purpose = VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL;
            destination = oldEmail;
        }

        // persist transition first
        request.startSecondFactor(type, now);
        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        if (saved.getSecondFactorType() == null || saved.getStatus() != EmailChangeStatus.SECOND_FACTOR_PENDING) {
            throw new EmailChangeStateCorruptedException(
                    saved.getId(),
                    saved.getUserId(),
                    "startSecondFactor did not persist expected state"
            );
        }

        // then send OTP
        createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.USER,
                command.userId().toString(),
                purpose,
                channel,
                destination,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null,
                null
        ));

        return new StartSecondFactorResult(saved.getId(), type, saved.getStatus());
    }
}