package com.timeeconomy.notification.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.timeeconomy.notification.application.notification.port.in.LocalSmsInboxUseCase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/_local/sms-inbox")
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
@RequiredArgsConstructor
public class LocalSmsInboxController {

    private final LocalSmsInboxUseCase inbox;

    @GetMapping
    public ResponseEntity<List<LocalSmsInboxUseCase.MessageSummary>> findRecent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok()
                .headers(noStoreHeaders())
                .body(inbox.findRecent(limit));
    }

    @PostMapping("/{challengeId}/consume")
    public ResponseEntity<ConsumeLocalSmsResponse> consume(@PathVariable UUID challengeId) {
        return inbox.consume(challengeId)
                .map(message -> ResponseEntity.ok()
                        .headers(noStoreHeaders())
                        .body(new ConsumeLocalSmsResponse(
                                message.challengeId(),
                                message.otp(),
                                message.expiresAt()
                        )))
                .orElseGet(() -> ResponseEntity.notFound()
                        .headers(noStoreHeaders())
                        .build());
    }

    private static HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store");
        headers.set(HttpHeaders.PRAGMA, "no-cache");
        return headers;
    }

    public record ConsumeLocalSmsResponse(
            UUID challengeId,
            String otp,
            Instant expiresAt
    ) {
        @Override
        public String toString() {
            return "ConsumeLocalSmsResponse[challengeId=" + challengeId
                    + ", otp=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }
}
