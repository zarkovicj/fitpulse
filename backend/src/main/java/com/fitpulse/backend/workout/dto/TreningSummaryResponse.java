package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.StatusTreninga;
import com.fitpulse.backend.workout.Trening;
import com.fitpulse.backend.workout.TreningSerija;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// za listu istorije, bez serija; ukupnaKilaza = zbir (ponavljanja × kilaža) završenih serija
public record TreningSummaryResponse(
        Long id,
        String templateNaziv,
        LocalDate datum,
        Instant startedAt,
        Instant finishedAt,
        StatusTreninga status,
        int brojVezbi,
        BigDecimal ukupnaKilaza
) {
    public static TreningSummaryResponse from(Trening trening) {
        BigDecimal volume = trening.getExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .filter(TreningSerija::isCompleted)
                .filter(set -> set.getKilaza() != null)
                .map(set -> set.getKilaza().multiply(BigDecimal.valueOf(set.getBrojPonavljanja())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TreningSummaryResponse(
                trening.getId(),
                trening.getTemplate() != null ? trening.getTemplate().getNaziv() : null,
                trening.getDatum(),
                trening.getStartedAt(),
                trening.getFinishedAt(),
                trening.getStatus(),
                trening.getExercises().size(),
                volume
        );
    }
}
