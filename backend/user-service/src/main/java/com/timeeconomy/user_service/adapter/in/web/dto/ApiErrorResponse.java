// auth-service/src/main/java/com/timeeconomy/auth_service/adapter/in/web/dto/ApiErrorResponse.java
package com.timeeconomy.user_service.adapter.in.web.dto;

public record ApiErrorResponse(
        boolean success,
        String service,
        String code,
        String message,
        int status,
        String path,
        String timestamp
) {}