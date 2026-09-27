package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.exercise.MuscleGroup;
import com.fitpulse.backend.workout.WorkoutExercise;

import java.util.List;

public record WorkoutExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        MuscleGroup muscleGroup,
        int position,
        int setCount,
        boolean completed,
        Integer restAfter,
        List<WorkoutSetResponse> sets
) {
    public static WorkoutExerciseResponse from(WorkoutExercise exercise) {
        return new WorkoutExerciseResponse(
                exercise.getId(),
                exercise.getExercise().getId(),
                exercise.getExercise().getName(),
                exercise.getExercise().getMuscleGroup(),
                exercise.getPosition(),
                exercise.getSetCount(),
                exercise.isCompleted(),
                exercise.getRestAfter(),
                exercise.getSets().stream().map(WorkoutSetResponse::from).toList()
        );
    }
}
