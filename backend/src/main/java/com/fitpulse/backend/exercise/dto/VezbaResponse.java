package com.fitpulse.backend.exercise.dto;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.exercise.Vezba;

public record VezbaResponse(
        Long id,
        String naziv,
        MisicnaGrupa misicnaGrupa,
        String slika,
        String opis,
        Long createdBy,
        boolean system
) {
    public static VezbaResponse from(Vezba vezba) {
        Long ownerId = vezba.getOwnerId();
        return new VezbaResponse(
                vezba.getId(),
                vezba.getNaziv(),
                vezba.getMisicnaGrupa(),
                vezba.getSlika(),
                vezba.getOpis(),
                ownerId,
                ownerId == null
        );
    }
}
