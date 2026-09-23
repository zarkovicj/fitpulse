package com.fitpulse.backend.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record BodyGoalRequest(
        @DecimalMin(value = "20", message = "Ciljna masa mora biti bar 20 kg")
        @DecimalMax(value = "500", message = "Ciljna masa može biti najviše 500 kg") BigDecimal masa,
        @DecimalMin(value = "1", message = "Procenat masti mora biti bar 1")
        @DecimalMax(value = "70", message = "Procenat masti može biti najviše 70") BigDecimal procenatMasti
) {
}
