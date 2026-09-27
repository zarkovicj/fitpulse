package com.fitpulse.backend.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartWorkoutRequest(
        Long templateId,
        @Size(max = 30, message = "Trening može imati najviše 30 vežbi")
        List<@Valid WorkoutExerciseRequest> exercises
) {
}
