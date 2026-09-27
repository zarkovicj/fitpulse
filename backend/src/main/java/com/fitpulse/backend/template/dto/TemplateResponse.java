package com.fitpulse.backend.template.dto;

import com.fitpulse.backend.template.Template;

import java.util.List;

public record TemplateResponse(
        Long id,
        String name,
        String description,
        Long createdBy,
        boolean system,
        List<TemplateExerciseResponse> exercises
) {
    public static TemplateResponse from(Template template) {
        Long ownerId = template.getOwnerId();
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                ownerId,
                ownerId == null,
                template.getExercises().stream().map(TemplateExerciseResponse::from).toList()
        );
    }
}
