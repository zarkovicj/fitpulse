package com.fitpulse.backend.account;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record AccountMailProperties(
        @NotBlank String from,
        @NotBlank String frontendUrl
) {
}
