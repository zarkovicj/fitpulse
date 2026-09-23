package com.fitpulse.backend.progress;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LicniRekordRepository extends JpaRepository<LicniRekord, Long> {

    List<LicniRekord> findByKorisnikIdAndVezbaIdIn(Long korisnikId, Collection<Long> vezbaIds);

    long countByKorisnikId(Long korisnikId);

    @EntityGraph(attributePaths = "vezba")
    @Query("""
            SELECT r FROM LicniRekord r
            WHERE r.korisnik.id = :korisnikId
            AND (:vezbaId IS NULL OR r.vezba.id = :vezbaId)
            AND (:treningId IS NULL OR r.treningId = :treningId)
            ORDER BY r.achievedAt DESC, r.id
            """)
    List<LicniRekord> search(@Param("korisnikId") Long korisnikId,
                             @Param("vezbaId") Long vezbaId,
                             @Param("treningId") Long treningId);
}
