package com.fitpulse.backend.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TreningSerijaRepository extends JpaRepository<TreningSerija, Long> {

    // null ako nema nijedne serije u periodu
    @Query("""
            SELECT SUM(s.kilaza * s.brojPonavljanja) FROM TreningSerija s
            JOIN s.treningVezba tv JOIN tv.trening t
            WHERE t.korisnik.id = :korisnikId
            AND t.status = :status
            AND s.completed = true
            AND t.datum >= :from
            """)
    BigDecimal sumVolumeSince(@Param("korisnikId") Long korisnikId,
                              @Param("status") StatusTreninga status,
                              @Param("from") LocalDate from);

    @Query("""
            SELECT s FROM TreningSerija s
            JOIN FETCH s.treningVezba tv JOIN FETCH tv.trening t
            WHERE t.korisnik.id = :korisnikId
            AND tv.vezba.id = :vezbaId
            AND t.status = :status
            AND s.completed = true
            ORDER BY t.finishedAt, t.id
            """)
    List<TreningSerija> findCompletedSetsForExercise(@Param("korisnikId") Long korisnikId,
                                                     @Param("vezbaId") Long vezbaId,
                                                     @Param("status") StatusTreninga status);
}
