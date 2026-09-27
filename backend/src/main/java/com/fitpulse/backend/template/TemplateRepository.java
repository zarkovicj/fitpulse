package com.fitpulse.backend.template;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    @Query("""
            SELECT DISTINCT t FROM Template t LEFT JOIN t.createdBy c
            WHERE c IS NULL OR c.id = :viewerId
            ORDER BY t.name
            """)
    List<Template> findVisible(@Param("viewerId") Long viewerId);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Template> findWithExercisesById(Long id);
}
