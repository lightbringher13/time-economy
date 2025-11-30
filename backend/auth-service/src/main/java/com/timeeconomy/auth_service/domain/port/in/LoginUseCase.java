package com.timeeconomy.auth_service.domain.port.in;

/**
 * 로그인 유스케이스 (도메인 포트, 인터페이스만 정의)
 *
 * - 아직 "어떻게" 검증하는지는 모른다.
 * - 단지 이메일/비밀번호 + 디바이스 정보를 받아서
 *   액세스 토큰/리프레시 토큰을 발급해 준다는 계약만 정의한다.
 */
public interface LoginUseCase {

    /**
     * 로그인 시도
     */
    LoginResult login(LoginCommand command);

    /**
     * 클라이언트가 보내는 입력값(커맨드)
     */
    record LoginCommand(
            String email,
            String password,
            String deviceInfo,
            String ipAddress,
            String userAgent
    ) {}

    /**
     * 도메인이 로그인 성공 시 돌려주는 결과(리절트)
     *
     * - accessToken: FE가 Authorization 헤더에 넣을 JWT
     * - refreshToken: 컨트롤러에서 HttpOnly 쿠키로 내려줄 원본 토큰 값
     * - userId / email: FE나 다른 서비스가 필요하면 사용
     * - familyId: 이 디바이스/브라우저의 가족 ID (세션 그룹)
     */
    record LoginResult(
        String accessToken,
        String refreshToken
) {}
}