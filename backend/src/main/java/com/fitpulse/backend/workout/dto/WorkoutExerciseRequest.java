package com.fitpulse.backend.workout.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkoutExerciseRequest(
        @NotNull(message = "Vežba je obavezna") Long exerciseId,
        @Min(value = 1, message = "Broj serija mora biti bar 1")
        @Max(value = 20, message = "Broj serija može biti najviše 20") Integer setCount,
        @Min(value = 1, message = "Broj ponavljanja mora biti bar 1")
        @Max(value = 1000, message = "Broj ponavljanja može biti najviše 1000") Integer reps,
        @DecimalMin(value = "0", message = "Kilaža ne može biti negativna")
        @DecimalMax(value = "1000", message = "Kilaža može biti najviše 1000 kg") BigDecimal weight
) {
}
