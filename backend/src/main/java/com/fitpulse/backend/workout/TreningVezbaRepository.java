package com.fitpulse.backend.workout;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TreningVezbaRepository extends JpaRepository<TreningVezba, Long> {

    @Query("""
            SELECT tv FROM TreningVezba tv JOIN tv.trening t
            WHERE t.korisnik.id = :korisnikId
            AND tv.vezba.id = :vezbaId
            AND t.status = :status
            ORDER BY t.finishedAt DESC, tv.id DESC
            """)
    List<TreningVezba> findLatestForUser(@Param("korisnikId") Long korisnikId,
                                         @Param("vezbaId") Long vezbaId,
                                         @Param("status") StatusTreninga status,
                                         Limit limit);
}
