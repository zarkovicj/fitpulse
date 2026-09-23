package com.fitpulse.backend.progress;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RekordKalkulatorTest {

    @Test
    void epley_withSingleRep_shouldReturnWeightItself() {
        assertThat(RekordKalkulator.epley(new BigDecimal("100"), 1)).isEqualByComparingTo("100.00");
    }

    @Test
    void epley_withMultipleReps_shouldApplyFormula() {
        assertThat(RekordKalkulator.epley(new BigDecimal("60"), 8)).isEqualByComparingTo("76.00");
        assertThat(RekordKalkulator.epley(new BigDecimal("70"), 6)).isEqualByComparingTo("84.00");
        assertThat(RekordKalkulator.epley(new BigDecimal("65"), 10)).isEqualByComparingTo("86.67");
    }

    @Test
    void hasWeight_shouldRejectNullAndZero() {
        assertThat(RekordKalkulator.hasWeight(null)).isFalse();
        assertThat(RekordKalkulator.hasWeight(BigDecimal.ZERO)).isFalse();
        assertThat(RekordKalkulator.hasWeight(new BigDecimal("2.5"))).isTrue();
    }
}
