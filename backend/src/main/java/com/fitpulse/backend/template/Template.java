package com.fitpulse.backend.template;

import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String naziv;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Korisnik createdBy;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redniBroj")
    private List<TemplateVezba> exercises = new ArrayList<>();

    public static Template create(String naziv, String opis, Korisnik createdBy) {
        Template template = new Template();
        template.naziv = naziv;
        template.opis = opis;
        template.createdBy = createdBy;
        return template;
    }

    public Long getOwnerId() {
        return createdBy != null ? createdBy.getId() : null;
    }

    public TemplateVezba addExercise(Vezba vezba, int brojSerija, int brojPonavljanja, BigDecimal kilaza) {
        int nextRedniBroj = exercises.stream().mapToInt(TemplateVezba::getRedniBroj).max().orElse(0) + 1;
        TemplateVezba item = TemplateVezba.create(this, vezba, brojSerija, brojPonavljanja, kilaza, nextRedniBroj);
        exercises.add(item);
        return item;
    }

    public void removeExercise(TemplateVezba item) {
        exercises.remove(item);
    }

    public void clearExercises() {
        exercises.clear();
    }
}
