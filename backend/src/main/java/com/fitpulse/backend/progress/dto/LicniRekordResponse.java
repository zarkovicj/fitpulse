package com.fitpulse.backend.progress.dto;

import com.fitpulse.backend.progress.LicniRekord;
import com.fitpulse.backend.progress.TipRekorda;

import java.math.BigDecimal;
import java.time.Instant;

public record LicniRekordResponse(
        Long id,
        Long vezbaId,
        String vezbaNaziv,
        TipRekorda tip,
        BigDecimal kilaza,
        int ponavljanja,
        BigDecimal estimated1rm,
        Instant achievedAt,
        Long treningId
) {
    public static LicniRekordResponse from(LicniRekord rekord) {
        return new LicniRekordResponse(
                rekord.getId(),
                rekord.getVezba().getId(),
                rekord.getVezba().getNaziv(),
                rekord.getTip(),
                rekord.getKilaza(),
                rekord.getPonavljanja(),
                rekord.getEstimated1rm(),
                rekord.getAchievedAt(),
                rekord.getTreningId()
        );
    }
}
