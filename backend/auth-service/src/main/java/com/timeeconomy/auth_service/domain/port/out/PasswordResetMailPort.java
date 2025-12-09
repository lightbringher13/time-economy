// domain.port.out

package com.timeeconomy.auth_service.domain.port.out;

public interface PasswordResetMailPort {

    /**
     * rawToken: the token user will receive in the link.
     * Adapter will turn it into a URL like:
     *   https://fe-host/reset-password?token=rawToken
     */
    void sendPasswordResetLink(String email, String rawToken);
}