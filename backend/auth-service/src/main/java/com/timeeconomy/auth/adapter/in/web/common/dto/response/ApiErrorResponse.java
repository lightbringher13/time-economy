// src/main/java/com/timeeconomy/auth/adapter/in/web/common/ApiErrorResponse.java
package com.timeeconomy.auth.adapter.in.web.common.dto.response;

public record ApiErrorResponse(
        String code,
        String message
) {}