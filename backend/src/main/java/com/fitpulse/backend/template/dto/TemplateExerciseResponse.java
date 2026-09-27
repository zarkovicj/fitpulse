package com.fitpulse.backend.template.dto;

import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.template.TemplateExercise;

import java.math.BigDecimal;

public record TemplateExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        MuscleGroup muscleGroup,
        int setCount,
        int reps,
        BigDecimal weight,
        int position
) {
    public static TemplateExerciseResponse from(TemplateExercise item) {
        return new TemplateExerciseResponse(
                item.getId(),
                item.getExercise().getId(),
                item.getExercise().getName(),
                item.getExercise().getMuscleGroup(),
                item.getSetCount(),
                item.getReps(),
                item.getWeight(),
                item.getPosition()
        );
    }
}
