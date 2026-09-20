package com.fitpulse.backend.user.dto;

import com.fitpulse.backend.user.Korisnik;
import com.fitpulse.backend.user.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(
        Long id,
        String ime,
        String prezime,
        String mail,
        LocalDate datumRodjenja,
        BigDecimal masa,
        BigDecimal visina,
        BigDecimal ciljnaMasa,
        Role role,
        Instant createdAt
) {
    public static UserResponse from(Korisnik korisnik) {
        return new UserResponse(
                korisnik.getId(),
                korisnik.getIme(),
                korisnik.getPrezime(),
                korisnik.getMail(),
                korisnik.getDatumRodjenja(),
                korisnik.getMasa(),
                korisnik.getVisina(),
                korisnik.getCiljnaMasa(),
                korisnik.getRole(),
                korisnik.getCreatedAt()
        );
    }
}
