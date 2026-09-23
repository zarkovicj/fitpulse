package com.fitpulse.backend.workout;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "trening_serija")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreningSerija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_trening_vezbe", nullable = false)
    private TreningVezba treningVezba;

    @Column(name = "redni_broj", nullable = false)
    private int redniBroj;

    @Column(name = "broj_ponavljanja", nullable = false)
    private int brojPonavljanja;

    private BigDecimal kilaza;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "rest_after")
    private Integer restAfter;

    static TreningSerija create(TreningVezba treningVezba, int redniBroj, int brojPonavljanja, BigDecimal kilaza) {
        TreningSerija set = new TreningSerija();
        set.treningVezba = treningVezba;
        set.redniBroj = redniBroj;
        set.brojPonavljanja = brojPonavljanja;
        set.kilaza = kilaza;
        return set;
    }

    void update(int brojPonavljanja, BigDecimal kilaza, boolean completed, Integer restAfter) {
        this.brojPonavljanja = brojPonavljanja;
        this.kilaza = kilaza;
        this.completed = completed;
        this.restAfter = restAfter;
    }

    void renumber(int redniBroj) {
        this.redniBroj = redniBroj;
    }
}
