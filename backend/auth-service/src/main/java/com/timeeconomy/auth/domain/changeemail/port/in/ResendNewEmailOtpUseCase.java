package com.timeeconomy.auth.domain.changeemail.port.in;

public interface ResendNewEmailOtpUseCase {
  void resend(ResendCommand command);

  record ResendCommand(Long userId, Long requestId) {}
}