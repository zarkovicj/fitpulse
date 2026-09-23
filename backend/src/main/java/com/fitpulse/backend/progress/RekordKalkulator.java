package com.fitpulse.backend.progress;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RekordKalkulator {

    private static final BigDecimal TRIDESET = BigDecimal.valueOf(30);

    private RekordKalkulator() {
    }

    // Epley: kilaža × (1 + ponavljanja / 30); za jedno ponavljanje 1RM je sama kilaža
    static BigDecimal epley(BigDecimal kilaza, int ponavljanja) {
        if (ponavljanja == 1) {
            return kilaza.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal faktor = BigDecimal.ONE.add(BigDecimal.valueOf(ponavljanja).divide(TRIDESET, 10, RoundingMode.HALF_UP));
        return kilaza.multiply(faktor).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean hasWeight(BigDecimal kilaza) {
        return kilaza != null && kilaza.signum() > 0;
    }

    static BigDecimal orZero(BigDecimal kilaza) {
        return kilaza != null ? kilaza : BigDecimal.ZERO;
    }
}
