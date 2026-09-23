package com.fitpulse.backend.progress;

import com.fitpulse.backend.user.Korisnik;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "body_goal")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.NONE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_korisnika", nullable = false, unique = true)
    private Korisnik korisnik;

    private BigDecimal masa;

    @Column(name = "procenat_masti")
    private BigDecimal procenatMasti;

    public static BodyGoal create(Korisnik korisnik) {
        BodyGoal goal = new BodyGoal();
        goal.korisnik = korisnik;
        return goal;
    }
}
