package com.fitpulse.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateUserRequest(
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotBlank(message = "Prezime je obavezno") String prezime,
        LocalDate datumRodjenja,
        BigDecimal masa,
        BigDecimal visina,
        BigDecimal ciljnaMasa
) {
}
