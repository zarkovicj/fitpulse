package com.fitpulse.backend.workout.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkoutSetRequest(
        @NotNull(message = "Broj ponavljanja je obavezan")
        @Min(value = 0, message = "Broj ponavljanja ne može biti negativan")
        @Max(value = 1000, message = "Broj ponavljanja može biti najviše 1000") Integer reps,
        @DecimalMin(value = "0", message = "Kilaža ne može biti negativna")
        @DecimalMax(value = "1000", message = "Kilaža može biti najviše 1000 kg") BigDecimal weight,
        @NotNull(message = "Polje completed je obavezno") Boolean completed,
        @Min(value = 0, message = "Odmor ne može biti negativan")
        @Max(value = 3600, message = "Odmor može biti najviše sat vremena") Integer restAfter
) {
}
