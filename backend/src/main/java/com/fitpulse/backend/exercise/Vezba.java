package com.fitpulse.backend.exercise;

import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vezba")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vezba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String naziv;

    @Enumerated(EnumType.STRING)
    @Column(name = "misicna_grupa", nullable = false, length = 30)
    private MisicnaGrupa misicnaGrupa;

    @Column(length = 500)
    private String slika;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Korisnik createdBy;

    public static Vezba create(String naziv, MisicnaGrupa misicnaGrupa, String slika, String opis, Korisnik createdBy) {
        Vezba vezba = new Vezba();
        vezba.naziv = naziv;
        vezba.misicnaGrupa = misicnaGrupa;
        vezba.slika = slika;
        vezba.opis = opis;
        vezba.createdBy = createdBy;
        return vezba;
    }

    public Long getOwnerId() {
        return createdBy != null ? createdBy.getId() : null;
    }
}
