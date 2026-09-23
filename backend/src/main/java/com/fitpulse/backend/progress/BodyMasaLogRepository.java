package com.fitpulse.backend.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyMasaLogRepository extends JpaRepository<BodyMasaLog, Long> {

    Optional<BodyMasaLog> findByKorisnikIdAndDatum(Long korisnikId, LocalDate datum);

    Optional<BodyMasaLog> findFirstByKorisnikIdOrderByDatumDesc(Long korisnikId);

    List<BodyMasaLog> findByKorisnikIdAndDatumBetweenOrderByDatum(Long korisnikId, LocalDate from, LocalDate to);
}
