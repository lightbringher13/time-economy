package com.timeeconomy.user.application.userprofile.service;

import com.timeeconomy.user.adapter.in.kafka.event.EmailChangeCommittedV1;
import com.timeeconomy.user.application.userprofile.port.in.HandleEmailChangeCommittedUseCase;
import com.timeeconomy.user.domain.userprofile.model.UserProfile;
import com.timeeconomy.user.domain.userprofile.port.out.UserProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HandleEmailChangeCommittedService implements HandleEmailChangeCommittedUseCase {

    private final UserProfileRepositoryPort userProfileRepositoryPort;

    @Override
    @Transactional
    public void handle(EmailChangeCommittedV1 event) {
        Long userId = event.userId();

        UserProfile profile = userProfileRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("UserProfile not found for userId=" + userId));

        LocalDateTime occurredAt = parseOccurredAt(event.occurredAt());

        profile.applyEmailChangeCommitted(
                event.oldEmail(),
                event.newEmail(),
                occurredAt
        );

        userProfileRepositoryPort.save(profile);
    }

    private LocalDateTime parseOccurredAt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // If auth sends ISO-8601 like "2025-12-20T10:15:49.132Z", use OffsetDateTime instead.
        // For now, assume LocalDateTime string without zone:
        return LocalDateTime.parse(raw);
    }
}