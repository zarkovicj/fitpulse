package com.fitpulse.backend.progress.dto;

import com.fitpulse.backend.progress.PersonalRecord;
import com.fitpulse.backend.progress.RecordType;

import java.math.BigDecimal;
import java.time.Instant;

public record PersonalRecordResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        RecordType type,
        BigDecimal weight,
        int reps,
        BigDecimal estimated1rm,
        Instant achievedAt,
        Long workoutId
) {
    public static PersonalRecordResponse from(PersonalRecord personalRecord) {
        return new PersonalRecordResponse(
                personalRecord.getId(),
                personalRecord.getExercise().getId(),
                personalRecord.getExercise().getName(),
                personalRecord.getType(),
                personalRecord.getWeight(),
                personalRecord.getReps(),
                personalRecord.getEstimated1rm(),
                personalRecord.getAchievedAt(),
                personalRecord.getWorkoutId()
        );
    }
}
