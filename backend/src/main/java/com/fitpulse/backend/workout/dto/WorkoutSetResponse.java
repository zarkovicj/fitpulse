package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.WorkoutSet;

import java.math.BigDecimal;

public record WorkoutSetResponse(
        Long id,
        int position,
        int reps,
        BigDecimal weight,
        boolean completed,
        Integer restAfter
) {
    public static WorkoutSetResponse from(WorkoutSet set) {
        return new WorkoutSetResponse(
                set.getId(),
                set.getPosition(),
                set.getReps(),
                set.getWeight(),
                set.isCompleted(),
                set.getRestAfter()
        );
    }
}
