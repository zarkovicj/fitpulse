package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// masa se ne menja ovde, nego preko /api/progress/weight
public record UpdateUserRequest(
        @NotBlank(message = "Ime je obavezno") @Size(max = 100, message = "Ime može imati najviše 100 karaktera") String firstName,
        @NotBlank(message = "Prezime je obavezno") @Size(max = 100, message = "Prezime može imati najviše 100 karaktera") String lastName,
        @Past(message = "Datum rođenja mora biti u prošlosti") LocalDate birthDate,
        @DecimalMin(value = "50", message = "Visina mora biti bar 50 cm")
        @DecimalMax(value = "260", message = "Visina može biti najviše 260 cm") BigDecimal height
) {
}
