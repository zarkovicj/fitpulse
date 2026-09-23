package com.fitpulse.backend.workout.dto;

import jakarta.validation.Valid;

import java.util.List;

// templateId ili lista vežbi (ad-hoc), ne oba
public record StartTreningRequest(
        Long templateId,
        List<@Valid TreningVezbaRequest> exercises
) {
}
