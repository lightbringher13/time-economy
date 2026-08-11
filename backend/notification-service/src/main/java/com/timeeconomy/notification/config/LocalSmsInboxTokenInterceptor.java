package com.timeeconomy.notification.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.timeeconomy.notification.adapter.in.web.LocalSmsInboxController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("sms-inbox & sms-mock & (local | docker | test) & !prod")
public class LocalSmsInboxTokenInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "X-Local-Sms-Inbox-Token";

    private final byte[] expectedToken;

    public LocalSmsInboxTokenInterceptor(LocalSmsInboxProperties properties) {
        this.expectedToken = properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)
                || !LocalSmsInboxController.class.isAssignableFrom(handlerMethod.getBeanType())) {
            return true;
        }

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");

        String suppliedToken = request.getHeader(TOKEN_HEADER);
        boolean authorized = suppliedToken != null && MessageDigest.isEqual(
                expectedToken,
                suppliedToken.getBytes(StandardCharsets.UTF_8)
        );

        if (authorized) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"FORBIDDEN\",\"message\":\"Local SMS inbox access denied.\"}"
        );
        return false;
    }
}
