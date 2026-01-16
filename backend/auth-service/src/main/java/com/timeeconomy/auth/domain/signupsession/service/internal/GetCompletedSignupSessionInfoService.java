// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/service/GetCompletedSignupSessionInfoService.java
package com.timeeconomy.auth.domain.signupsession.service.internal;

import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionNotCompletedException;
import com.timeeconomy.auth.domain.signupsession.exception.SignupSessionNotFoundException;
import com.timeeconomy.auth.domain.signupsession.model.SignupSession;
import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;
import com.timeeconomy.auth.domain.signupsession.port.in.internal.GetCompletedSignupSessionInfoUseCase;
import com.timeeconomy.auth.domain.signupsession.port.out.SignupSessionStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCompletedSignupSessionInfoService implements GetCompletedSignupSessionInfoUseCase {

    private final SignupSessionStorePort store;

    @Override
    public Result getCompletedInfo(Query query) {
        SignupSession s = store.findById(query.sessionId())
                .orElseThrow(() -> new SignupSessionNotFoundException("SignupSession not found"));

        if (s.getState() != SignupSessionState.COMPLETED) {
            throw new SignupSessionNotCompletedException("SignupSession is not COMPLETED");
        }

        return new Result(
                s.getEmail(),
                s.getPhoneNumber(),
                s.getName(),
                s.getGender(),
                s.getBirthDate(),
                s.getState()
        );
    }
}