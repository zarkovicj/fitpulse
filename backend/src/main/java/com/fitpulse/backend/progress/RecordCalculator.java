package com.fitpulse.backend.progress;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RecordCalculator {

    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    static final int MAX_REPS_FOR_1RM = 30;

    private RecordCalculator() {
    }

    static boolean canEstimate1rm(BigDecimal weight, int reps) {
        return hasWeight(weight) && reps >= 1 && reps <= MAX_REPS_FOR_1RM;
    }

    // Epley: kilaza × (1 + ponavljanja / 30)
    // za jedno ponavljanje 1RM je sama kilaza
    static BigDecimal epley(BigDecimal weight, int reps) {
        if (reps == 1) {
            return weight.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(reps).divide(THIRTY, 10, RoundingMode.HALF_UP));
        return weight.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean hasWeight(BigDecimal weight) {
        return weight != null && weight.signum() > 0;
    }

    static BigDecimal orZero(BigDecimal weight) {
        return weight != null ? weight : BigDecimal.ZERO;
    }
}
