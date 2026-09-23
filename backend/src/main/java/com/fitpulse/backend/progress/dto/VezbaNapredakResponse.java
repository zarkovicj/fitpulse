package com.fitpulse.backend.progress.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// jedna tačka na grafiku napretka za vežbu = jedan završen trening
public record VezbaNapredakResponse(
        Long treningId,
        LocalDate datum,
        BigDecimal maxKilaza,
        BigDecimal najbolji1rm,
        int maxPonavljanja,
        BigDecimal volumen
) {
}
