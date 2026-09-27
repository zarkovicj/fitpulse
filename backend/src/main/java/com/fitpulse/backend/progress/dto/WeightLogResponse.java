package com.fitpulse.backend.progress.dto;

import com.fitpulse.backend.progress.BodyWeightLog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightLogResponse(LocalDate date, BigDecimal weight) {

    public static WeightLogResponse from(BodyWeightLog log) {
        return new WeightLogResponse(log.getDate(), log.getWeight());
    }
}
