package com.fitpulse.backend.progress.dto;

import java.math.BigDecimal;

public record ProgressSummaryResponse(
        long totalWorkouts,
        long workoutsThisWeek,
        BigDecimal volumeLast30Days,
        long recordCount,
        BigDecimal currentWeight,
        BigDecimal goalWeight,
        BigDecimal goalBodyFatPercent
) {
}
