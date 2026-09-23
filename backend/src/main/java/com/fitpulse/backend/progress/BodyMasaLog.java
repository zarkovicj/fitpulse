package com.fitpulse.backend.progress;

import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "body_masa_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyMasaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_korisnika", nullable = false)
    private Korisnik korisnik;

    @Setter
    @Column(nullable = false)
    private BigDecimal masa;

    @Column(nullable = false)
    private LocalDate datum;

    public static BodyMasaLog create(Korisnik korisnik, LocalDate datum, BigDecimal masa) {
        BodyMasaLog log = new BodyMasaLog();
        log.korisnik = korisnik;
        log.datum = datum;
        log.masa = masa;
        return log;
    }
}
