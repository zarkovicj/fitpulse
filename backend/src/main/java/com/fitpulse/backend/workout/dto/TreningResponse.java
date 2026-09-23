package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.StatusTreninga;
import com.fitpulse.backend.workout.Trening;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TreningResponse(
        Long id,
        Long templateId,
        String templateNaziv,
        LocalDate datum,
        Instant startedAt,
        Instant finishedAt,
        StatusTreninga status,
        List<TreningVezbaResponse> exercises
) {
    public static TreningResponse from(Trening trening) {
        return new TreningResponse(
                trening.getId(),
                trening.getTemplateId(),
                trening.getTemplate() != null ? trening.getTemplate().getNaziv() : null,
                trening.getDatum(),
                trening.getStartedAt(),
                trening.getFinishedAt(),
                trening.getStatus(),
                trening.getExercises().stream().map(TreningVezbaResponse::from).toList()
        );
    }
}
