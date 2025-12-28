package com.timeeconomy.user.application.userprofile.service;

import com.timeeconomy.contracts.auth.v1.EmailChangeCommittedV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleEmailChangeCommittedUseCase;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HandleEmailChangeCommittedService implements HandleEmailChangeCommittedUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Override
    @Transactional
    public void handle(EmailChangeCommittedV1 event) {

        Long userId = event.getUserId();

        UserProfile profile = userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("UserProfile not found for userId=" + userId));

        String oldEmail = toStr(event.getOldEmail());
        String newEmail = toStr(event.getNewEmail());

        // logicalType timestamp-millis -> Instant
        Instant occurredAt = event.getOccurredAtEpochMillis();

        profile.applyEmailChangeCommitted(oldEmail, newEmail, occurredAt);

        userProfileRepositoryPort.save(profile);
    }

    private static String toStr(Object v) {
        return v == null ? null : v.toString();
    }
}