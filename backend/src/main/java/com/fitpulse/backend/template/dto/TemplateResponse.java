package com.fitpulse.backend.template.dto;

import com.fitpulse.backend.template.Template;

import java.util.List;

public record TemplateResponse(
        Long id,
        String naziv,
        String opis,
        Long createdBy,
        boolean system,
        List<TemplateVezbaResponse> exercises
) {
    public static TemplateResponse from(Template template) {
        Long ownerId = template.getOwnerId();
        return new TemplateResponse(
                template.getId(),
                template.getNaziv(),
                template.getOpis(),
                ownerId,
                ownerId == null,
                template.getExercises().stream().map(TemplateVezbaResponse::from).toList()
        );
    }
}
