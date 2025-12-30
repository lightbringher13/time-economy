package com.timeeconomy.auth.domain.changeemail.service.internal;

import com.timeeconomy.auth.domain.changeemail.port.in.internal.GetEmailChangeRequestInternalUseCase;
import com.timeeconomy.auth.domain.changeemail.port.out.EmailChangeRequestRepositoryPort;
import com.timeeconomy.auth.domain.exception.EmailChangeRequestNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetEmailChangeRequestInternalService implements GetEmailChangeRequestInternalUseCase {

    private final EmailChangeRequestRepositoryPort emailChangeRequestRepositoryPort;

    @Override
    public Result getById(Long requestId) {
        var req = emailChangeRequestRepositoryPort.findById(requestId)
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(null, requestId));

        return new Result(
                req.getId(),
                req.getUserId(),
                req.getOldEmail(),
                req.getNewEmail(),
                req.getStatus(),
                req.getExpiresAt(),
                req.getCreatedAt(),
                req.getUpdatedAt()
        );
    }
}