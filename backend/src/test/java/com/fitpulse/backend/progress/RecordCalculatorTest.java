package com.fitpulse.backend.progress;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RecordCalculatorTest {

    @Test
    void epley_withSingleRep_shouldReturnWeightItself() {
        assertThat(RecordCalculator.epley(new BigDecimal("100"), 1)).isEqualByComparingTo("100.00");
    }

    @Test
    void epley_withMultipleReps_shouldApplyFormula() {
        assertThat(RecordCalculator.epley(new BigDecimal("60"), 8)).isEqualByComparingTo("76.00");
        assertThat(RecordCalculator.epley(new BigDecimal("70"), 6)).isEqualByComparingTo("84.00");
        assertThat(RecordCalculator.epley(new BigDecimal("65"), 10)).isEqualByComparingTo("86.67");
    }

    @Test
    void hasWeight_shouldRejectNullAndZero() {
        assertThat(RecordCalculator.hasWeight(null)).isFalse();
        assertThat(RecordCalculator.hasWeight(BigDecimal.ZERO)).isFalse();
        assertThat(RecordCalculator.hasWeight(new BigDecimal("2.5"))).isTrue();
    }

    @Test
    void canEstimate1rm_shouldAllowOnlyWeightedSetsUpTo30Reps() {
        BigDecimal kg = new BigDecimal("100");
        assertThat(RecordCalculator.canEstimate1rm(kg, 1)).isTrue();
        assertThat(RecordCalculator.canEstimate1rm(kg, 30)).isTrue();
        assertThat(RecordCalculator.canEstimate1rm(kg, 31)).isFalse();
        assertThat(RecordCalculator.canEstimate1rm(kg, 0)).isFalse();
        assertThat(RecordCalculator.canEstimate1rm(null, 5)).isFalse();
    }
}
