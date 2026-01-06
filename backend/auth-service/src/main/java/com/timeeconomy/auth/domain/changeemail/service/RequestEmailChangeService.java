package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RequestEmailChangeService implements RequestEmailChangeUseCase {

    private static final Duration CHANGE_EMAIL_TTL = Duration.ofMinutes(30);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final Clock clock;
    private final CreateOtpUseCase createOtpUseCase;

    @Override
    @Transactional
    public RequestEmailChangeResult requestEmailChange(RequestEmailChangeCommand command) {
        final Instant now = Instant.now(clock);

        final Long userId = command.userId();
        final String currentPassword = command.currentPassword();

        final String newEmail = normalizeEmail(command.newEmail());
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("newEmail is required");
        }

        AuthUser user = authUserRepositoryPort.findById(userId)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));

        if (!passwordEncoderPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }

        authUserRepositoryPort.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new EmailAlreadyUsedException("Email is already in use");
            }
        });

        EmailChangeRequest active = emailChangeRequestRepositoryPort.findActiveByUserId(userId).orElse(null);

        // If active exists but expired, mark expired and treat as none
        if (active != null && active.isExpired(now)) {
            active.markExpired(now);
            emailChangeRequestRepositoryPort.save(active);
            active = null;
        }

        // ✅ Resume behavior
        if (active != null) {
            // Same target newEmail? -> resume current state instead of restarting
            if (newEmail.equalsIgnoreCase(active.getNewEmail())) {

                // Only resend OTP if we are actually waiting for new-email verification
                if (active.getStatus() == EmailChangeStatus.PENDING) {
                    createNewEmailOtp(userId, newEmail);
                }

                return new RequestEmailChangeResult(
                        active.getId(),
                        maskEmail(newEmail),
                        active.getStatus()
                );
            }

            // Different newEmail -> cancel old active request and start a new one
            active.markCanceled(now);
            emailChangeRequestRepositoryPort.save(active);
        }

        Instant expiresAt = now.plus(CHANGE_EMAIL_TTL);

        EmailChangeRequest created = EmailChangeRequest.create(
                userId,
                user.getEmail(),
                newEmail,
                null,
                expiresAt,
                now
        );

        EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(created);

        createNewEmailOtp(userId, newEmail);

        return new RequestEmailChangeResult(
                saved.getId(),
                maskEmail(newEmail),
                saved.getStatus()
        );
    }

    private void createNewEmailOtp(Long userId, String newEmail) {
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
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return null;
        return raw.trim().toLowerCase();
    }

    private static String maskEmail(String email) {
        if (email == null) return null;
        int atIdx = email.indexOf('@');
        if (atIdx < 0) return "***";
        if (atIdx <= 1) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx);
    }
}