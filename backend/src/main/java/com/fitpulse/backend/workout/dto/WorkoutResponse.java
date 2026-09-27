package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.WorkoutStatus;
import com.fitpulse.backend.workout.Workout;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(
        Long id,
        Long templateId,
        String templateName,
        LocalDate date,
        Instant startedAt,
        Instant finishedAt,
        WorkoutStatus status,
        List<WorkoutExerciseResponse> exercises
) {
    public static WorkoutResponse from(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getTemplateId(),
                workout.getTemplate() != null ? workout.getTemplate().getName() : null,
                workout.getDate(),
                workout.getStartedAt(),
                workout.getFinishedAt(),
                workout.getStatus(),
                workout.getExercises().stream().map(WorkoutExerciseResponse::from).toList()
        );
    }
}
