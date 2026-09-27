package com.fitpulse.backend.exercise.dto;

import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.exercise.Exercise;

public record ExerciseResponse(
        Long id,
        String name,
        MuscleGroup muscleGroup,
        String imageUrl,
        String description,
        String videoUrl,
        Long createdBy,
        boolean system
) {
    public static ExerciseResponse from(Exercise exercise) {
        Long ownerId = exercise.getOwnerId();
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getMuscleGroup(),
                exercise.getImageKey() != null ? "/api/images/" + exercise.getImageKey() : null,
                exercise.getDescription(),
                exercise.getVideoUrl(),
                ownerId,
                ownerId == null
        );
    }
}
