package com.fitpulse.backend.exercise.dto;

import com.fitpulse.backend.exercise.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(
        @NotBlank(message = "Naziv je obavezan") @Size(max = 150, message = "Naziv može imati najviše 150 karaktera") String name,
        @NotNull(message = "Mišićna grupa je obavezna") MuscleGroup muscleGroup,
        @Size(max = 2000, message = "Opis može imati najviše 2000 karaktera") String description,
        @Size(max = 500, message = "Link do videa je predugačak")
        @Pattern(regexp = "^https?://\\S+$", message = "Link do videa mora da počinje sa http:// ili https://") String videoUrl
) {
}
