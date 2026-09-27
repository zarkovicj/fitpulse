package com.fitpulse.backend.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    @Query("""
            SELECT e FROM Exercise e LEFT JOIN e.createdBy c
            WHERE (c IS NULL OR c.id = :viewerId)
            AND (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup)
            AND LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            ORDER BY e.name
            """)
    List<Exercise> search(@Param("viewerId") Long viewerId,
                       @Param("muscleGroup") MuscleGroup muscleGroup,
                       @Param("search") String search);
}
