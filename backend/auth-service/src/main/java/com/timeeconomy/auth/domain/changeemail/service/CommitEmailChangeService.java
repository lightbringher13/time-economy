package com.timeeconomy.auth.domain.changeemail.service;

import com.timeeconomy.auth.domain.auth.model.AuthUser;
import com.timeeconomy.auth.domain.auth.port.out.AuthUserRepositoryPort;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeRequest;
import com.timeeconomy.auth.domain.changeemail.model.EmailChangeStatus;
import com.timeeconomy.auth.domain.changeemail.model.payload.EmailChangeCommittedPayload;
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
import com.timeeconomy.auth.domain.changeemail.port.in.CommitEmailChangeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;

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
        Instant now = Instant.now(clock);
        String lockKey = "change-email:user:" + command.userId();

        try (LockHandle lock = distributedLockPort.acquireLock(lockKey)) {

            EmailChangeRequest request = emailChangeRequestRepositoryPort
                    .findByIdAndUserId(command.requestId(), command.userId())
                    .orElseThrow(() -> new EmailChangeRequestNotFoundException(command.userId(), command.requestId()));

            if (request.isExpired(now)) {
                request.markExpired(now);
                emailChangeRequestRepositoryPort.save(request);
                throw new InvalidSecondFactorCodeException();
            }

            // ✅ idempotent: already completed
            if (request.getStatus() == EmailChangeStatus.COMPLETED) {
                return new CommitEmailChangeResult(request.getId(), request.getNewEmail());
            }

            if (request.getStatus() != EmailChangeStatus.READY_TO_COMMIT) {
                throw new InvalidSecondFactorCodeException(); // or a new InvalidEmailChangeStateException
            }

            AuthUser user = authUserRepositoryPort.findById(command.userId())
                    .orElseThrow(() -> new AuthUserNotFoundException(command.userId()));

            if (!user.getEmail().equals(request.getOldEmail())) {
                throw new EmailChangeEmailMismatchException();
            }

            // Commit change
            user.updateEmail(request.getNewEmail(), now);
            authUserRepositoryPort.save(user);

            request.markCompleted(now);
            EmailChangeRequest saved = emailChangeRequestRepositoryPort.save(request);

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

            return new CommitEmailChangeResult(saved.getId(), saved.getNewEmail());
        }
    }
}