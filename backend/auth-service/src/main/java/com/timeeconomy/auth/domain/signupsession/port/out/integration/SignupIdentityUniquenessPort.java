// signupsession/port/out/integration/SignupIdentityUniquenessPort.java
package com.timeeconomy.auth.domain.signupsession.port.out.integration;

public interface SignupIdentityUniquenessPort {

  enum Outcome {
    AVAILABLE,
    EMAIL_ALREADY_USED,
    PHONE_ALREADY_USED,
    INVALID_INPUT
  }

  record Result(Outcome outcome) {}

  Result checkEmail(String normalizedEmail);
  Result checkPhone(String normalizedPhone);
}