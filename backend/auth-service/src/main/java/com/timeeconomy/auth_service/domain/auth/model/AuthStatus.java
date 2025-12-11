package com.timeeconomy.auth_service.domain.auth.model;

public enum AuthStatus {
    ACTIVE,     // 정상 로그인 가능
    PENDING,    // 이메일 미인증 / 초기 가입 상태
    LOCKED,     // 로그인 실패 누적 등으로 잠금
    DELETED     // 탈퇴/삭제 처리
}