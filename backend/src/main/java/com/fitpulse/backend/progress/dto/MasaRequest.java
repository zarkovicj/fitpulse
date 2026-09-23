package com.fitpulse.backend.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MasaRequest(
        @NotNull(message = "Masa je obavezna")
        @DecimalMin(value = "20", message = "Masa mora biti bar 20 kg")
        @DecimalMax(value = "500", message = "Masa može biti najviše 500 kg") BigDecimal masa
) {
}
