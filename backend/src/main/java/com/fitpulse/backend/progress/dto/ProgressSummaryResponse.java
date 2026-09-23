package com.fitpulse.backend.progress.dto;

import java.math.BigDecimal;

public record ProgressSummaryResponse(
        long ukupnoTreninga,
        long treninziOveNedelje,
        BigDecimal ukupnaKilaza30Dana,
        long brojRekorda,
        BigDecimal trenutnaMasa,
        BigDecimal ciljnaMasa,
        BigDecimal ciljniProcenatMasti
) {
}
