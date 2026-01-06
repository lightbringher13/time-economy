package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.port.in.GetActiveEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetActiveEmailChangeService implements GetActiveEmailChangeUseCase {

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional
    public Optional<GetActiveEmailChangeResult> getActive(GetActiveEmailChangeCommand command) {
        final Instant now = Instant.now(clock);

        EmailChangeRequest request = emailChangeRequestRepositoryPort
                .findActiveByUserId(command.userId())
                .orElse(null);

        if (request == null) {
            return Optional.empty();
        }

        // 1) best-effort expiry materialization
        if (request.isExpired(now) && request.getStatus() != EmailChangeStatus.EXPIRED) {
            EmailChangeStatus before = request.getStatus();
            request.markExpired(now);

            if (before == request.getStatus()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "isExpired=true but markExpired did not transition state"
                );
            }

            request = emailChangeRequestRepositoryPort.save(request);

            // if it became expired, it's no longer "active"
            if (request.getStatus() == EmailChangeStatus.EXPIRED) {
                return Optional.empty();
            }
        }

        final EmailChangeStatus status = request.getStatus();

        // 2) strict invariants (same style as your GetStatus)
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

        if ((status == EmailChangeStatus.READY_TO_COMMIT
                || status == EmailChangeStatus.COMPLETED)
                && isBlank(request.getOldEmail())) {
            throw new EmailChangeStateCorruptedException(
                    request.getId(),
                    request.getUserId(),
                    status + " but oldEmail is missing"
            );
        }

        return Optional.of(new GetActiveEmailChangeResult(
                request.getId(),
                status,
                request.getSecondFactorType(),
                maskEmail(request.getNewEmail()),
                request.getExpiresAt()
        ));
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