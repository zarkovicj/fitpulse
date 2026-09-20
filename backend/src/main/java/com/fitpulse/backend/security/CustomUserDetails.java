package com.fitpulse.backend.security;

import com.fitpulse.backend.user.Korisnik;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Korisnik korisnik;

    public CustomUserDetails(Korisnik korisnik) {
        this.korisnik = korisnik;
    }

    public Long getId() {
        return korisnik.getId();
    }

    public Korisnik getKorisnik() {
        return korisnik;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + korisnik.getRole().name()));
    }

    @Override
    public String getPassword() {
        return korisnik.getHashPassword();
    }

    @Override
    public String getUsername() {
        return korisnik.getMail();
    }
}
