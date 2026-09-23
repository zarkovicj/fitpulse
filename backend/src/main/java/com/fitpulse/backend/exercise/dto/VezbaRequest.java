package com.fitpulse.backend.exercise.dto;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VezbaRequest(
        @NotBlank(message = "Naziv je obavezan") String naziv,
        @NotNull(message = "Mišićna grupa je obavezna") MisicnaGrupa misicnaGrupa,
        String slika,
        String opis,
        boolean system
) {
}
