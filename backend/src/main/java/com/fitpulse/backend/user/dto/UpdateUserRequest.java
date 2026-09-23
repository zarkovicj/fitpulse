package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;

// masa se ne menja ovde, nego kroz dnevnik merenja (/api/progress/weight)
public record UpdateUserRequest(
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotBlank(message = "Prezime je obavezno") String prezime,
        @Past(message = "Datum rođenja mora biti u prošlosti") LocalDate datumRodjenja,
        @DecimalMin(value = "50", message = "Visina mora biti bar 50 cm")
        @DecimalMax(value = "260", message = "Visina može biti najviše 260 cm") BigDecimal visina
) {
}
