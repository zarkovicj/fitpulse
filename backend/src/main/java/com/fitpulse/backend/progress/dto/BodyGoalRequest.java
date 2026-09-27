package com.fitpulse.backend.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record BodyGoalRequest(
        @DecimalMin(value = "40", message = "Ciljna masa mora biti bar 40 kg")
        @DecimalMax(value = "200", message = "Ciljna masa može biti najviše 200 kg") BigDecimal weight,
        @DecimalMin(value = "1", message = "Procenat masti mora biti bar 1")
        @DecimalMax(value = "70", message = "Procenat masti može biti najviše 70") BigDecimal bodyFatPercent
) {
}
