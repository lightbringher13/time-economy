package com.timeeconomy.auth_service.domain.port.in;

public interface ChangePasswordUseCase {

    /**
     * 로그인된 사용자가 자신의 비밀번호를 변경할 때 사용하는 유즈케이스.
     *
     * @param command authUserId: 현재 로그인한 auth_user PK
     *                currentPassword: 현재 비밀번호(평문)
     *                newPassword: 새 비밀번호(평문)
     */
    void changePassword(Command command);

    record Command(
            Long authUserId,
            String currentPassword,
            String newPassword
    ) {}
}