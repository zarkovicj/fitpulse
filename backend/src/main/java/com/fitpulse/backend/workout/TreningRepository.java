package com.fitpulse.backend.workout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TreningRepository extends JpaRepository<Trening, Long> {

    boolean existsByKorisnikIdAndStatus(Long korisnikId, StatusTreninga status);

    long countByKorisnikIdAndStatus(Long korisnikId, StatusTreninga status);

    long countByKorisnikIdAndStatusAndDatumGreaterThanEqual(Long korisnikId, StatusTreninga status, LocalDate datum);

    @EntityGraph(attributePaths = {"exercises", "exercises.vezba"})
    Optional<Trening> findWithExercisesByIdAndKorisnikId(Long id, Long korisnikId);

    @EntityGraph(attributePaths = {"exercises", "exercises.vezba"})
    Optional<Trening> findFirstByKorisnikIdAndStatus(Long korisnikId, StatusTreninga status);

    @Query(value = """
            SELECT t FROM Trening t
            WHERE t.korisnik.id = :korisnikId
            ORDER BY t.startedAt DESC, t.id DESC
            """,
            countQuery = "SELECT COUNT(t) FROM Trening t WHERE t.korisnik.id = :korisnikId")
    Page<Trening> findHistory(@Param("korisnikId") Long korisnikId, Pageable pageable);
}
