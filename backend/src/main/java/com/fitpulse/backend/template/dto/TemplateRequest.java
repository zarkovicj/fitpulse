package com.fitpulse.backend.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

// exercises == null na PUT-u znači "menjaj samo naziv i opis"
public record TemplateRequest(
        @NotBlank(message = "Naziv je obavezan") String naziv,
        String opis,
        boolean system,
        List<@Valid TemplateVezbaRequest> exercises
) {
}
