package com.fitpulse.backend.template;

import com.fitpulse.backend.exercise.Vezba;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "template_vezba")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateVezba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_template", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vezbe", nullable = false)
    private Vezba vezba;

    @Column(name = "broj_serija", nullable = false)
    private int brojSerija;

    @Column(name = "broj_ponavljanja", nullable = false)
    private int brojPonavljanja;

    private BigDecimal kilaza;

    @Setter(AccessLevel.NONE)
    @Column(name = "redni_broj", nullable = false)
    private int redniBroj;

    static TemplateVezba create(Template template, Vezba vezba, int brojSerija, int brojPonavljanja,
                                BigDecimal kilaza, int redniBroj) {
        TemplateVezba item = new TemplateVezba();
        item.template = template;
        item.vezba = vezba;
        item.brojSerija = brojSerija;
        item.brojPonavljanja = brojPonavljanja;
        item.kilaza = kilaza;
        item.redniBroj = redniBroj;
        return item;
    }
}
