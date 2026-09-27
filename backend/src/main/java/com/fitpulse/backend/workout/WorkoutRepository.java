package com.fitpulse.backend.workout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    boolean existsByUserIdAndStatus(Long userId, WorkoutStatus status);

    long countByUserIdAndStatus(Long userId, WorkoutStatus status);

    long countByUserIdAndStatusAndDateGreaterThanEqual(Long userId, WorkoutStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findWithExercisesByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findFirstByUserIdAndStatus(Long userId, WorkoutStatus status);

    @Query(value = """
            SELECT w FROM Workout w
            WHERE w.user.id = :userId
            ORDER BY w.startedAt DESC, w.id DESC
            """,
            countQuery = "SELECT COUNT(w) FROM Workout w WHERE w.user.id = :userId")
    Page<Workout> findHistory(@Param("userId") Long userId, Pageable pageable);
}
