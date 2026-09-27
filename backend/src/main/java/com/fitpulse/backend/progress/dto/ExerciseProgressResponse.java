package com.fitpulse.backend.progress.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExerciseProgressResponse(
        Long workoutId,
        LocalDate date,
        BigDecimal maxWeight,
        BigDecimal best1rm,
        int maxReps,
        BigDecimal volume
) {
}
