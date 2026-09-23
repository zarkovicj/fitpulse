package com.fitpulse.backend.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VezbaRepository extends JpaRepository<Vezba, Long> {

    @Query("""
            SELECT v FROM Vezba v LEFT JOIN v.createdBy c
            WHERE (:viewerId IS NULL OR c IS NULL OR c.id = :viewerId)
            AND (:muscleGroup IS NULL OR v.misicnaGrupa = :muscleGroup)
            AND LOWER(v.naziv) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY v.naziv
            """)
    List<Vezba> search(@Param("viewerId") Long viewerId,
                       @Param("muscleGroup") MisicnaGrupa muscleGroup,
                       @Param("search") String search);
}
