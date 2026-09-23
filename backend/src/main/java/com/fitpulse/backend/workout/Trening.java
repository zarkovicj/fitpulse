package com.fitpulse.backend.workout;

import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trening")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_template")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_korisnika", nullable = false)
    private Korisnik korisnik;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTreninga status;

    @OneToMany(mappedBy = "trening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redniBroj")
    private List<TreningVezba> exercises = new ArrayList<>();

    public static Trening start(Korisnik korisnik, Template template) {
        Trening trening = new Trening();
        trening.korisnik = korisnik;
        trening.template = template;
        trening.startedAt = Instant.now();
        trening.datum = LocalDate.now();
        trening.status = StatusTreninga.IN_PROGRESS;
        return trening;
    }

    public TreningVezba addExercise(Vezba vezba) {
        TreningVezba exercise = TreningVezba.create(this, vezba, exercises.size() + 1);
        exercises.add(exercise);
        return exercise;
    }

    public void removeExercise(TreningVezba exercise) {
        exercises.remove(exercise);
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).renumber(i + 1);
        }
    }

    public void finish() {
        status = StatusTreninga.COMPLETED;
        finishedAt = Instant.now();
    }

    public void cancel() {
        status = StatusTreninga.CANCELLED;
        finishedAt = Instant.now();
    }

    public boolean isInProgress() {
        return status == StatusTreninga.IN_PROGRESS;
    }

    public Long getTemplateId() {
        return template != null ? template.getId() : null;
    }
}
