package com.fitpulse.backend.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// exercises == null na PUT: menjaj samo naziv i opis
public record TemplateRequest(
        @NotBlank(message = "Naziv je obavezan") @Size(max = 150, message = "Naziv može imati najviše 150 karaktera") String name,
        @Size(max = 2000, message = "Opis može imati najviše 2000 karaktera") String description,
        @Size(max = 30, message = "Šablon može imati najviše 30 vežbi")
        List<@Valid TemplateExerciseRequest> exercises
) {
}
