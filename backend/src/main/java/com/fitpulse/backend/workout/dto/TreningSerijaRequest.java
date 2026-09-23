package com.fitpulse.backend.workout.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TreningSerijaRequest(
        @NotNull(message = "Broj ponavljanja je obavezan") @Min(value = 0, message = "Broj ponavljanja ne može biti negativan") Integer brojPonavljanja,
        @DecimalMin(value = "0", message = "Kilaža ne može biti negativna") BigDecimal kilaza,
        @NotNull(message = "Polje completed je obavezno") Boolean completed,
        @Min(value = 0, message = "Odmor ne može biti negativan") Integer restAfter
) {
}
