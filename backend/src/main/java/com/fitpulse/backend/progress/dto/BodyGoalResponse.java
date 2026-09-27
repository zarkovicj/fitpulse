package com.fitpulse.backend.progress.dto;

import java.math.BigDecimal;

public record BodyGoalResponse(BigDecimal weight, BigDecimal bodyFatPercent) {
}
