// domain.port.in

package com.timeeconomy.auth_service.domain.port.in;

public interface RequestPasswordResetUseCase {

    record Command(String email) {}

    /**
     * For security, we don't reveal if email exists.
     * Just say "request accepted".
     */
    record Result(boolean accepted) {}

    Result requestReset(Command command);
}