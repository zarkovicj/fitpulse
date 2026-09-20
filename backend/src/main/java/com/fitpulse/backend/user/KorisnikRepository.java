package com.fitpulse.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KorisnikRepository extends JpaRepository<Korisnik, Long> {
    Optional<Korisnik> findByMail(String mail);
    boolean existsByMail(String mail);
}
