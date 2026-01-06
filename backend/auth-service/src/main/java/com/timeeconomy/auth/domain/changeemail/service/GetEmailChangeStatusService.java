package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.in.GetEmailChangeStatusUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GetEmailChangeStatusService implements GetEmailChangeStatusUseCase {

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional
    public GetEmailChangeStatusResult getStatus(GetEmailChangeStatusCommand command) {
        final Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findByIdAndUserId(command.requestId(), command.userId())
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

        // 1) best-effort expiry materialization
        if (request.isExpired(now) && request.getStatus() != EmailChangeStatus.EXPIRED) {
            EmailChangeStatus before = request.getStatus();
            request.markExpired(now);

            // guard: domain refused to transition even though isExpired() was true
            if (before == request.getStatus()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "isExpired=true but markExpired did not transition state"
                );
            }

            request = emailChangeRequestRepositoryPort.save(request);
        }

        final EmailChangeStatus status = request.getStatus();

        // 2) invariants for each state (strict, but very helpful)
        // PENDING+ should have newEmail (otherwise FE can't even display it)
        if ((status == EmailChangeStatus.PENDING
                || status == EmailChangeStatus.NEW_EMAIL_VERIFIED
                || status == EmailChangeStatus.SECOND_FACTOR_PENDING
                || status == EmailChangeStatus.READY_TO_COMMIT
                || status == EmailChangeStatus.COMPLETED)
                && isBlank(request.getNewEmail())) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    status + " but newEmail is missing"
            );
        }

        // SECOND_FACTOR_PENDING+ must have secondFactorType
        if ((status == EmailChangeStatus.SECOND_FACTOR_PENDING
                || status == EmailChangeStatus.READY_TO_COMMIT
                || status == EmailChangeStatus.COMPLETED)
                && request.getSecondFactorType() == null) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    status + " but secondFactorType is null"
            );
        }

        // READY_TO_COMMIT/COMPLETED should have oldEmail too (commit safety)
        if ((status == EmailChangeStatus.READY_TO_COMMIT
                || status == EmailChangeStatus.COMPLETED)
                && isBlank(request.getOldEmail())) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    status + " but oldEmail is missing"
            );
        }

        return new GetEmailChangeStatusResult(
                request.getId(),
                status,
                request.getSecondFactorType(),
                maskEmail(request.getNewEmail()),
                request.getExpiresAt()
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int atIdx = email.indexOf('@');
        if (atIdx < 0) return "***";
        if (atIdx <= 1) return "***" + email.substring(atIdx);
        return email.charAt(0) + "***" + email.substring(atIdx);
    }
}