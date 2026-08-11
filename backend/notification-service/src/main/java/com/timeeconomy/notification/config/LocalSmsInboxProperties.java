package com.timeeconomy.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "notification.sms.inbox")
public record LocalSmsInboxProperties(
        @NotBlank @Size(min = 24, max = 256) String token,
        @DefaultValue("100") @Min(1) @Max(1_000) int capacity
) {
    @Override
    public String toString() {
        return "LocalSmsInboxProperties[token=<redacted>, capacity=" + capacity + "]";
    }
}
