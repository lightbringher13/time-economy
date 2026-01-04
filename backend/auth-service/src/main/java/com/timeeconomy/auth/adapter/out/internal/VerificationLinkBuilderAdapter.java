package com.timeeconomy.auth.adapter.out.internal;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.out.internal.VerificationLinkBuilderPort;

@Component
public class VerificationLinkBuilderAdapter implements VerificationLinkBuilderPort {

    private final Map<VerificationPurpose, String> baseUrls;

    public VerificationLinkBuilderAdapter(
            // Example config keys (you can rename):
            @Value("${app.links.password-reset-base-url}") String passwordResetBaseUrl,
            @Value("${app.links.email-verify-base-url}") String emailVerifyBaseUrl
    ) {
        EnumMap<VerificationPurpose, String> m = new EnumMap<>(VerificationPurpose.class);
        m.put(VerificationPurpose.PASSWORD_RESET, passwordResetBaseUrl);
        m.put(VerificationPurpose.SIGNUP_EMAIL, emailVerifyBaseUrl);
        this.baseUrls = m;
    }

    @Override
    public String buildLinkUrl(String rawToken, VerificationPurpose purpose) {
        String baseUrl = baseUrls.get(purpose);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("link baseUrl not configured for purpose=" + purpose);
        }

        // Builds: {baseUrl}?token=xxxxx   (safe escaping)
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }
}