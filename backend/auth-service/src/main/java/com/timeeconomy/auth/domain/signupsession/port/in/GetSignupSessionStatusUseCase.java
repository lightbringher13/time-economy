// backend/auth-service/src/main/java/com/timeeconomy/auth/domain/signupsession/port/in/GetSignupSessionStatusUseCase.java
package com.timeeconomy.auth.domain.signupsession.port.in;

import java.time.LocalDate;
import java.util.UUID;

import com.timeeconomy.auth.domain.signupsession.model.SignupSessionState;

public interface GetSignupSessionStatusUseCase {

  record Query(UUID sessionId) {} // may be null

  record Result(
      boolean exists,
      String email,
      boolean emailVerified,
      boolean emailOtpPending,    // ✅ NEW
      String phoneNumber,
      boolean phoneVerified,
      boolean phoneOtpPending,    // ✅ NEW
      String name,
      String gender,
      LocalDate birthDate,
      SignupSessionState state
  ) {}

  Result getStatus(Query query);
}