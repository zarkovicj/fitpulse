package com.fitpulse.backend.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static Korisnik register(String ime, String prezime, String mail, String hashPassword, LocalDate datumRodjenja) {
        Korisnik korisnik = new Korisnik();
        korisnik.ime = ime;
        korisnik.prezime = prezime;
        korisnik.mail = mail;
        korisnik.hashPassword = hashPassword;
        korisnik.datumRodjenja = datumRodjenja;
        korisnik.role = Role.USER;
        return korisnik;
    }

    public static Korisnik createAdmin(String ime, String prezime, String mail, String hashPassword) {
        Korisnik korisnik = new Korisnik();
        korisnik.ime = ime;
        korisnik.prezime = prezime;
        korisnik.mail = mail;
        korisnik.hashPassword = hashPassword;
        korisnik.role = Role.ADMIN;
        return korisnik;
    }
}
