package com.fitpulse.backend.workout.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// vrednosti su opcione: ako vežba postoji u istoriji, koriste se vrednosti iz prošlog treninga
public record TreningVezbaRequest(
        @NotNull(message = "Vežba je obavezna") Long vezbaId,
        @Min(value = 1, message = "Broj serija mora biti bar 1") Integer brojSerija,
        @Min(value = 1, message = "Broj ponavljanja mora biti bar 1") Integer brojPonavljanja,
        @DecimalMin(value = "0", message = "Kilaža ne može biti negativna") BigDecimal kilaza
) {
}
