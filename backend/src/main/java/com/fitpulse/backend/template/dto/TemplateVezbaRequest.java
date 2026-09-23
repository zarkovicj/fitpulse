package com.fitpulse.backend.template.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TemplateVezbaRequest(
        @NotNull(message = "Vežba je obavezna") Long vezbaId,
        @NotNull(message = "Broj serija je obavezan") @Min(value = 1, message = "Broj serija mora biti bar 1") Integer brojSerija,
        @NotNull(message = "Broj ponavljanja je obavezan") @Min(value = 1, message = "Broj ponavljanja mora biti bar 1") Integer brojPonavljanja,
        @DecimalMin(value = "0", message = "Kilaža ne može biti negativna") BigDecimal kilaza
) {
}
