package com.fitpulse.backend.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WeightRequest(
        @NotNull(message = "Masa je obavezna")
        @DecimalMin(value = "40", message = "Masa mora biti bar 40 kg")
        @DecimalMax(value = "200", message = "Masa može biti najviše 200 kg") BigDecimal weight
) {
}
