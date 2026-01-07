// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/changeemail/service/ResendNewEmailOtpService.java
package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeForbiddenException;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeInvalidStateException;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.SecondFactorType;
import com.timeeconomy.auth.domain.changeemail.port.in.ResendNewEmailOtpUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
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
public class ResendNewEmailOtpService implements ResendNewEmailOtpUseCase {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final AuthUserRepositoryPort authUserRepositoryPort; // ✅ for PHONE destination
    private final CreateOtpUseCase createOtpUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public void resend(ResendCommand command) {
        Instant now = Instant.now(clock);

        EmailChangeRequest req = emailChangeRequestRepositoryPort.findById(command.requestId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.requestId()));

        if (!req.getUserId().equals(command.userId())) {
            throw new EmailChangeForbiddenException();
        }

        // expire & persist if it changed
        if (req.expireIfNeeded(now)) {
            emailChangeRequestRepositoryPort.save(req);
            throw new EmailChangeInvalidStateException("resend OTP", req.getStatus());
        }

        // ✅ branch by state
        if (req.getStatus() == EmailChangeStatus.PENDING) {
            resendNewEmailOtp(command.userId(), req);
            return;
        }

        if (req.getStatus() == EmailChangeStatus.SECOND_FACTOR_PENDING) {
            resendSecondFactorOtp(command.userId(), req);
            return;
        }

        throw new EmailChangeInvalidStateException("resend OTP", req.getStatus());
    }

    private void resendNewEmailOtp(Long userId, EmailChangeRequest req) {
        createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                req.getNewEmail(),
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null,
                null
        ));
    }

    private void resendSecondFactorOtp(Long userId, EmailChangeRequest req) {
        SecondFactorType type = req.getSecondFactorType();
        if (type == null) {
            throw new EmailChangeInvalidStateException("resend second factor OTP (missing type)", req.getStatus());
        }

        if (type == SecondFactorType.OLD_EMAIL) {
            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    userId.toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_OLD_EMAIL, // ✅ use whatever you use in StartSecondFactorService
                    VerificationChannel.EMAIL,
                    req.getOldEmail(),
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
            return;
        }

        if (type == SecondFactorType.PHONE) {
            AuthUser user = authUserRepositoryPort.findById(userId)
                    .orElseThrow(() -> new AuthUserNotFoundException(userId));

            String phone = user.getPhoneNumber(); // ✅ adjust getter to your model
            if (phone == null || phone.isBlank()) {
                throw new EmailChangeInvalidStateException("resend SMS OTP (no phone)", req.getStatus());
            }

            createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                    VerificationSubjectType.USER,
                    userId.toString(),
                    VerificationPurpose.CHANGE_EMAIL_2FA_PHONE, // ✅ same purpose as above
                    VerificationChannel.SMS,
                    phone,
                    OTP_TTL,
                    OTP_MAX_ATTEMPTS,
                    null,
                    null
            ));
            return;
        }

        throw new EmailChangeInvalidStateException("resend second factor OTP (unknown type)", req.getStatus());
    }
}