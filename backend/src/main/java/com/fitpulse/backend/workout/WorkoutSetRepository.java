package com.fitpulse.backend.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    // null ako nema nijedne serije u periodu
    @Query("""
            SELECT SUM(s.weight * s.reps) FROM WorkoutSet s
            JOIN s.workoutExercise we JOIN we.workout w
            WHERE w.user.id = :userId
            AND w.status = :status
            AND s.completed = true
            AND w.date >= :from
            """)
    BigDecimal sumVolumeSince(@Param("userId") Long userId,
                              @Param("status") WorkoutStatus status,
                              @Param("from") LocalDate from);

    @Query("""
            SELECT s FROM WorkoutSet s
            JOIN FETCH s.workoutExercise we JOIN FETCH we.workout w
            WHERE w.user.id = :userId
            AND we.exercise.id = :exerciseId
            AND w.status = :status
            AND s.completed = true
            ORDER BY w.finishedAt, w.id
            """)
    List<WorkoutSet> findCompletedSetsForExercise(@Param("userId") Long userId,
                                                     @Param("exerciseId") Long exerciseId,
                                                     @Param("status") WorkoutStatus status);
}
