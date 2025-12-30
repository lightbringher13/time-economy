package com.timeeconomy.auth.adapter.in.web.changeemail.internal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.timeeconomy.auth.domain.changeemail.port.in.internal.GetEmailChangeRequestInternalUseCase;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/email-change-requests")
public class InternalEmailChangeRequestController {

    private final GetEmailChangeRequestInternalUseCase useCase;

    @Value("${app.internal.token}")
    private String internalToken;

    @GetMapping("/{requestId}")
    public ResponseEntity<GetEmailChangeRequestInternalUseCase.Result> get(
            @PathVariable Long requestId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            HttpServletRequest http
    ) {
        if (token == null || token.isBlank() || !token.equals(internalToken)) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(useCase.getById(requestId));
    }
}