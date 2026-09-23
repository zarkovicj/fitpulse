package com.fitpulse.backend.template.dto;

import com.fitpulse.backend.exercise.MisicnaGrupa;
import com.fitpulse.backend.template.TemplateVezba;

import java.math.BigDecimal;

public record TemplateVezbaResponse(
        Long id,
        Long vezbaId,
        String vezbaNaziv,
        MisicnaGrupa misicnaGrupa,
        int brojSerija,
        int brojPonavljanja,
        BigDecimal kilaza,
        int redniBroj
) {
    public static TemplateVezbaResponse from(TemplateVezba item) {
        return new TemplateVezbaResponse(
                item.getId(),
                item.getVezba().getId(),
                item.getVezba().getNaziv(),
                item.getVezba().getMisicnaGrupa(),
                item.getBrojSerija(),
                item.getBrojPonavljanja(),
                item.getKilaza(),
                item.getRedniBroj()
        );
    }
}
