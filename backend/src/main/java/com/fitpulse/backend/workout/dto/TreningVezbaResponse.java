package com.fitpulse.backend.workout.dto;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.workout.TreningVezba;

import java.util.List;

public record TreningVezbaResponse(
        Long id,
        Long vezbaId,
        String vezbaNaziv,
        MisicnaGrupa misicnaGrupa,
        int redniBroj,
        int brojSerija,
        boolean completed,
        Integer restAfter,
        List<TreningSerijaResponse> sets
) {
    public static TreningVezbaResponse from(TreningVezba exercise) {
        return new TreningVezbaResponse(
                exercise.getId(),
                exercise.getVezba().getId(),
                exercise.getVezba().getNaziv(),
                exercise.getVezba().getMisicnaGrupa(),
                exercise.getRedniBroj(),
                exercise.getBrojSerija(),
                exercise.isCompleted(),
                exercise.getRestAfter(),
                exercise.getSets().stream().map(TreningSerijaResponse::from).toList()
        );
    }
}
