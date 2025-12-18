// domain/common/notification/model/NotificationMessage.java
package com.timeeconomy.auth_service.domain.common.notification.model;

import com.timeeconomy.auth_service.domain.verification.model.VerificationChannel;
import com.timeeconomy.auth_service.domain.verification.model.VerificationPurpose;

import java.util.Map;

public record NotificationMessage(
        VerificationChannel channel,
        String destination,
        String templateKey,
        Map<String, Object> vars
) {
    // VERIFICATION
    public static NotificationMessage otp(
            VerificationChannel channel,
            String destination,
            VerificationPurpose purpose,
            String code,
            int ttlMinutes
    ) {
        return new NotificationMessage(
                channel,
                destination,
                "VERIFICATION_OTP",
                Map.of("code", code, "ttlMinutes", ttlMinutes, "purpose", purpose.name())
        );
    }

    public static NotificationMessage link(
            VerificationChannel channel,
            String destination,
            VerificationPurpose purpose,
            String linkUrl,
            int ttlMinutes
    ) {
        return new NotificationMessage(
                channel,
                destination,
                "VERIFICATION_LINK",
                Map.of("linkUrl", linkUrl, "ttlMinutes", ttlMinutes, "purpose", purpose.name())
        );
    }

    // EMAIL CHANGE
    public static NotificationMessage emailChangedOld(String oldEmail, String newEmail) {
        return new NotificationMessage(
                VerificationChannel.EMAIL,
                oldEmail,
                "EMAIL_CHANGED_OLD",
                Map.of("newEmail", newEmail)
        );
    }

    public static NotificationMessage emailChangedNew(String newEmail) {
        return new NotificationMessage(
                VerificationChannel.EMAIL,
                newEmail,
                "EMAIL_CHANGED_NEW",
                Map.of()
        );
    }

    // SECURITY
    public static NotificationMessage securityAlert(
            String destinationEmail,
            String alertKey,
            Map<String, Object> vars
    ) {
        return new NotificationMessage(
                VerificationChannel.EMAIL,
                destinationEmail,
                alertKey,
                vars
        );
    }
}