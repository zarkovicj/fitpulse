package com.fitpulse.backend.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikeSearchTest {

    @Test
    void escape_shouldTreatWildcardsLiterally() {
        assertThat(LikeSearch.escape("100%")).isEqualTo("100\\%");
        assertThat(LikeSearch.escape("a_b")).isEqualTo("a\\_b");
        assertThat(LikeSearch.escape("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    void escape_shouldTrimAndNeverReturnNull() {
        assertThat(LikeSearch.escape("  bench ")).isEqualTo("bench");
        assertThat(LikeSearch.escape(null)).isEmpty();
    }
}
