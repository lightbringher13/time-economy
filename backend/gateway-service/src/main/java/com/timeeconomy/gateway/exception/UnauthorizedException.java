package com.timeeconomy.gateway.exception;

public class UnauthorizedException extends RuntimeException {

    private final String code; // FE에 내려보낼 에러 코드

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    // fallback constructor
    public UnauthorizedException(String message) {
        super(message);
        this.code = "UNAUTHORIZED";
    }

    public String getCode() {
        return code;
    }
}