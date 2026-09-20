package com.fitpulse.backend.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "korisnik")
@Getter
@Setter
@NoArgsConstructor
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ime;

    @Column(nullable = false, length = 100)
    private String prezime;

    @Column(nullable = false, unique = true)
    private String mail;

    @Column(name = "hash_password", nullable = false)
    private String hashPassword;

    @Column(name = "datum_rodjenja")
    private LocalDate datumRodjenja;

    private BigDecimal masa;

    private BigDecimal visina;

    @Column(name = "ciljna_masa")
    private BigDecimal ciljnaMasa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
