package com.fitpulse.backend.progress.dto;

import com.fitpulse.backend.progress.BodyMasaLog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MasaLogResponse(LocalDate datum, BigDecimal masa) {

    public static MasaLogResponse from(BodyMasaLog log) {
        return new MasaLogResponse(log.getDatum(), log.getMasa());
    }
}
