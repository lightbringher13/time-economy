package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.port.in.RequestEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.common.security.port.PasswordEncoderPort;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailAlreadyUsedException;
import com.timeeconomy.auth.domain.exception.InvalidCurrentPasswordException;
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
public class RequestEmailChangeService implements RequestEmailChangeUseCase {


    private static final Duration CHANGE_EMAIL_TTL_MINUTES = Duration.ofMinutes(30);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    private final Clock clock;

    private final CreateOtpUseCase createOtpUseCase;

    // ============ 1) Request email change ============

    @Override
    @Transactional
    public RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command) {
        Instant now = Instant.now(clock);

        Long userId = command.userId();
        String currentPassword = command.currentPassword();
        String newEmailRaw = command.newEmail();

        AuthUser user = authUserRepositoryPort.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        if (!passwordEncoderPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        String newEmail = newEmailRaw.trim().toLowerCase();

        authUserRepositoryPort.findByEmail(newEmail).ifPresent(existing -> {
            throw new EmailAlreadyUsedException("Email is already in use");
        });

        // cancel active request
        emailChangeRequestRepositoryPort.findActiveByUserId(userId)
                .ifPresent(existing -> {
                    existing.markCanceled(now);
                    emailChangeRequestRepositoryPort.save(existing);
                });

        Instant expiresAt = now.plus(CHANGE_EMAIL_TTL_MINUTES);
        EmailChangeRequest request = EmailChangeRequest.create(
                userId,
                user.getEmail(),
                newEmail,
                null,
                expiresAt,
                now
        );

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

        // create OTP for NEW email
        createOtpUseCase.createOtp(new CreateOtpUseCase.CreateOtpCommand(
                VerificationSubjectType.USER,
                userId.toString(),
                VerificationPurpose.CHANGE_EMAIL_NEW,
                VerificationChannel.EMAIL,
                newEmail,
                OTP_TTL,
                OTP_MAX_ATTEMPTS,
                null,
                null
        ));

        return new RequestEmailChangeResult(saved.getId(), maskEmail(newEmail));
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx);
    }
}