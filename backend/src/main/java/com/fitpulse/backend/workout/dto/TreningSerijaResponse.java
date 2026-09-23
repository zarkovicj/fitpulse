package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.workout.TreningSerija;

import java.math.BigDecimal;

public record TreningSerijaResponse(
        Long id,
        int redniBroj,
        int brojPonavljanja,
        BigDecimal kilaza,
        boolean completed,
        Integer restAfter
) {
    public static TreningSerijaResponse from(TreningSerija set) {
        return new TreningSerijaResponse(
                set.getId(),
                set.getRedniBroj(),
                set.getBrojPonavljanja(),
                set.getKilaza(),
                set.isCompleted(),
                set.getRestAfter()
        );
    }
}
