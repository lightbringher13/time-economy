// backend/auth-service/src/main/java/com/timeeconomy/auth/adapter/in/web/signupsession/internal/InternalCompletedSignupSessionController.java
package com.timeeconomy.auth.adapter.in.internal.signupsession;

import com.timeeconomy.auth.adapter.in.internal.signupsession.dto.response.CompletedSignupSessionResponse;
import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionNotCompletedException;
import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.port.in.GetCompletedSignupSessionInfoUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/signup-sessions")
public class InternalCompletedSignupSessionController {

    private final GetCompletedSignupSessionInfoUseCase useCase;

    @Value("${app.internal.token}")
    private String internalToken;

    @GetMapping("/{sessionId}/completed")
    public ResponseEntity<CompletedSignupSessionResponse> getCompleted(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            HttpServletRequest http
    ) {
        if (token == null || token.isBlank() || !token.equals(internalToken)) {
            return ResponseEntity.status(401).build();
        }

        try {
            var r = useCase.getCompletedInfo(new GetCompletedSignupSessionInfoUseCase.Query(sessionId));
            return ResponseEntity.ok(new CompletedSignupSessionResponse(
                    r.email(), r.phoneNumber(), r.name(), r.gender(), r.birthDate(), r.state()
            ));
        } catch (SignupSessionNotFoundException e) {
            return ResponseEntity.status(404).build();
        } catch (SignupSessionNotCompletedException e) {
            return ResponseEntity.status(409).build();
        }
    }
}