// src/main/java/com/timeeconomy/notification/adapter/out/brevo/BrevoEmailSenderAdapter.java
package com.timeeconomy.notification.adapter.out.brevo;

import com.timeeconomy.notification.application.notification.port.out.EmailSenderPort;
import com.timeeconomy.notification.application.notification.exception.EmailSendFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BrevoEmailSenderAdapter implements EmailSenderPort {

    @Qualifier("brevoRestClient")
    private final RestClient brevoRestClient;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.templates.welcome-email}")
    private int welcomeEmailTemplateId;

    @Value("${brevo.templates.email-change-new}")
    private int emailChangeNewTemplateId;

    @Value("${brevo.templates.email-change-old}")
    private int emailChangeOldTemplateId;

    @Value("${brevo.templates.otp-mail}")
    private int createVerificationOtpTemplateId;

    @Override
    public EmailSendResult sendTemplate(EmailSendCommand cmd) {
        int templateId = resolveTemplateId(cmd.templateKey());

        Map<String, Object> to = (cmd.toName() == null || cmd.toName().isBlank())
                ? Map.of("email", cmd.toEmail())
                : Map.of("email", cmd.toEmail(), "name", cmd.toName());

        Map<String, Object> body = Map.of(
                "to", List.of(to),
                "templateId", templateId,
                "params", cmd.params() == null ? Map.of() : cmd.params()
        );

        try {
            @SuppressWarnings("rawtypes")
            Map res = brevoRestClient.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String msgId = (res == null) ? null : String.valueOf(res.get("messageId"));
            return new EmailSendResult("BREVO", msgId);

        } catch (RestClientResponseException e) {
            // Brevo returned non-2xx (you get status + response body)
            throw new EmailSendFailedException(
                    "Brevo API failed: status=" + e.getStatusCode() + " body=" + safe(e.getResponseBodyAsString()),
                    e
            );
        } catch (Exception e) {
            // network timeout, DNS, etc.
            throw new EmailSendFailedException("Brevo call failed: " + safe(e.getMessage()), e);
        }
    }

    private int resolveTemplateId(String templateKey) {
        return switch (templateKey) {
            case "WELCOME_EMAIL" -> welcomeEmailTemplateId;
            case "EMAIL_CHANGE_NEW" -> emailChangeNewTemplateId;
            case "EMAIL_CHANGE_OLD" -> emailChangeOldTemplateId;
            case "OTP_EMAIL" -> createVerificationOtpTemplateId;
            default -> throw new IllegalArgumentException("Unknown templateKey: " + templateKey);
        };
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.length() > 800 ? s.substring(0, 800) : s;
    }
}