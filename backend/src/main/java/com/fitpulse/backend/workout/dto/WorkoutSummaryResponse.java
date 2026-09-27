package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.WorkoutStatus;
import com.fitpulse.backend.workout.Workout;
import com.fitpulse.backend.workout.WorkoutSet;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WorkoutSummaryResponse(
        Long id,
        String templateName,
        LocalDate date,
        Instant startedAt,
        Instant finishedAt,
        WorkoutStatus status,
        int exerciseCount,
        BigDecimal totalVolume
) {
    public static WorkoutSummaryResponse from(Workout workout) {
        BigDecimal volume = workout.getExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .filter(WorkoutSet::isCompleted)
                .filter(set -> set.getWeight() != null)
                .map(set -> set.getWeight().multiply(BigDecimal.valueOf(set.getReps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new WorkoutSummaryResponse(
                workout.getId(),
                workout.getTemplate() != null ? workout.getTemplate().getName() : null,
                workout.getDate(),
                workout.getStartedAt(),
                workout.getFinishedAt(),
                workout.getStatus(),
                workout.getExercises().size(),
                volume
        );
    }
}
