package com.fitpulse.backend.workout;

import com.fitpulse.backend.exercise.Vezba;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trening_vezba")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreningVezba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_treninga", nullable = false)
    private Trening trening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vezbe", nullable = false)
    private Vezba vezba;

    @Column(name = "broj_serija", nullable = false)
    private int brojSerija;

    @Column(name = "redni_broj", nullable = false)
    private int redniBroj;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "rest_after")
    private Integer restAfter;

    @OneToMany(mappedBy = "treningVezba", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redniBroj")
    private List<TreningSerija> sets = new ArrayList<>();

    static TreningVezba create(Trening trening, Vezba vezba, int redniBroj) {
        TreningVezba exercise = new TreningVezba();
        exercise.trening = trening;
        exercise.vezba = vezba;
        exercise.redniBroj = redniBroj;
        return exercise;
    }

    public TreningSerija addSet(int brojPonavljanja, BigDecimal kilaza) {
        TreningSerija set = TreningSerija.create(this, sets.size() + 1, brojPonavljanja, kilaza);
        sets.add(set);
        refresh();
        return set;
    }

    public void removeSet(TreningSerija set) {
        sets.remove(set);
        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).renumber(i + 1);
        }
        refresh();
    }

    public void updateSet(TreningSerija set, int brojPonavljanja, BigDecimal kilaza, boolean completed, Integer restAfter) {
        set.update(brojPonavljanja, kilaza, completed, restAfter);
        refresh();
    }

    void renumber(int redniBroj) {
        this.redniBroj = redniBroj;
    }

    // broj_serija i completed su izvedeni iz serija, drže se u sinhronizaciji ovde
    private void refresh() {
        brojSerija = sets.size();
        completed = !sets.isEmpty() && sets.stream().allMatch(TreningSerija::isCompleted);
    }
}
