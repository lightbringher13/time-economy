package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.exception.EmailChangeStateCorruptedException;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.payload.EmailChangeCommittedPayload;
import com.timeeconomy.auth.domain.changeemail.port.in.CommitEmailChangeUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.common.lock.port.DistributedLockPort;
import com.timeeconomy.auth.domain.common.lock.port.LockHandle;
import com.timeeconomy.auth.domain.exception.AuthUserNotFoundException;
import com.timeeconomy.auth.domain.exception.EmailChangeEmailMismatchException;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import com.timeeconomy.auth.domain.exception.InvalidSecondFactorCodeException;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxPayloadSerializerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CommitEmailChangeService implements CommitEmailChangeUseCase {

    private static final String OUTBOX_AGGREGATE_TYPE = "EmailChangeRequest";
    private static final String EVENT_EMAIL_CHANGE_COMMITTED = "EmailChangeCommitted.v1";

    private final AuthUserRepositoryPort authUserRepositoryPort;
    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;
    private final DistributedLockPort distributedLockPort;
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final OutboxPayloadSerializerPort outboxPayloadSerializerPort;
    private final Clock clock;

    @Override
    @Transactional
    public CommitEmailChangeResult commit(CommitEmailChangeCommand command) {
        final Instant now = Instant.now(clock);
        final String lockKey = "change-email:user:" + command.userId();

        try (LockHandle lock = distributedLockPort.acquireLock(lockKey)) {

            EmailChangeRequest request = emailChangeRequestRepositoryPort
                    .findByIdAndUserId(command.requestId(), command.userId())
                    .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

            // 1) expired → persist EXPIRED best-effort then fail
            if (request.isExpired(now)) {
                request.markExpired(now);
                emailChangeRequestRepositoryPort.save(request);
                throw new InvalidSecondFactorCodeException(); // later: InvalidEmailChangeRequestException
            }

            // 2) terminal states → fail fast
            if (request.getStatus() == EmailChangeStatus.CANCELED
                    || request.getStatus() == EmailChangeStatus.EXPIRED) {
                throw new InvalidSecondFactorCodeException();
            }

            // 3) idempotent: already completed
            if (request.getStatus() == EmailChangeStatus.COMPLETED) {
                String newEmail = request.getNewEmail();
                if (newEmail == null || newEmail.isBlank()) {
                    throw new EmailChangeStateCorruptedException(
                            request.getId(),
                            request.getUserId(),
                            "COMPLETED but newEmail is missing"
                    );
                }
                return new CommitEmailChangeResult(request.getId(), newEmail, request.getStatus());
            }

            // 4) only allowed state to commit
            if (request.getStatus() != EmailChangeStatus.READY_TO_COMMIT) {
                throw new InvalidSecondFactorCodeException(); // later: InvalidEmailChangeStateException
            }

            // 5) invariants for READY_TO_COMMIT
            String oldEmail = request.getOldEmail();
            if (oldEmail == null || oldEmail.isBlank()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "READY_TO_COMMIT but oldEmail is missing"
                );
            }

            String newEmail = request.getNewEmail();
            if (newEmail == null || newEmail.isBlank()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "READY_TO_COMMIT but newEmail is missing"
                );
            }

            if (request.getSecondFactorType() == null) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "READY_TO_COMMIT but secondFactorType is null"
                );
            }

            AuthUser user = authUserRepositoryPort.findById(command.userId())
                    .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

            // 6) safety check: user email changed during flow
            if (!oldEmail.equalsIgnoreCase(user.getEmail())) {
                throw new EmailChangeEmailMismatchException();
            }

            // 7) commit change
            user.updateEmail(newEmail, now);
            authUserRepositoryPort.save(user);

            // 8) guard against silent no-op transition
            EmailChangeStatus before = request.getStatus();
            request.markCompleted(now);
            if (before == request.getStatus()) {
                throw new EmailChangeStateCorruptedException(
                        request.getId(),
                        request.getUserId(),
                        "markCompleted did not transition state"
                );
            }

            EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

            if (saved.getStatus() != EmailChangeStatus.COMPLETED) {
                throw new EmailChangeStateCorruptedException(
                        saved.getId(),
                        saved.getUserId(),
                        "commit saved but status is not COMPLETED"
                );
            }

            // 9) outbox publish
            String payloadJson = outboxPayloadSerializerPort.serialize(
                    new EmailChangeCommittedPayload(
                            saved.getId().toString(),
                            saved.getUserId(),
                            saved.getOldEmail(),
                            saved.getNewEmail(),
                            now
                    )
            );

            outboxEventRepositoryPort.save(
                    OutboxEvent.newPending(
                            OUTBOX_AGGREGATE_TYPE,
                            saved.getId().toString(),
                            EVENT_EMAIL_CHANGE_COMMITTED,
                            payloadJson,
                            now
                    )
            );

            return new CommitEmailChangeResult(saved.getId(), saved.getNewEmail(), saved.getStatus());
        }
    }
}