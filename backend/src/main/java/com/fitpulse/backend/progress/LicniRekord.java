package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "licni_rekord")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LicniRekord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_korisnika", nullable = false)
    private Korisnik korisnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vezbe", nullable = false)
    private Vezba vezba;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipRekorda tip;

    private BigDecimal kilaza;

    @Column(nullable = false)
    private int ponavljanja;

    @Column(name = "estimated_1rm")
    private BigDecimal estimated1rm;

    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;

    // samo id, bez veze ka Trening entitetu, da progress i workout ne zavise jedan od drugog u krug
    @Column(name = "id_treninga")
    private Long treningId;

    public static LicniRekord create(Korisnik korisnik, Vezba vezba, TipRekorda tip) {
        LicniRekord rekord = new LicniRekord();
        rekord.korisnik = korisnik;
        rekord.vezba = vezba;
        rekord.tip = tip;
        return rekord;
    }

    public void record(int ponavljanja, BigDecimal kilaza, BigDecimal estimated1rm, Long treningId, Instant achievedAt) {
        this.ponavljanja = ponavljanja;
        this.kilaza = kilaza;
        this.estimated1rm = estimated1rm;
        this.treningId = treningId;
        this.achievedAt = achievedAt;
    }
}
