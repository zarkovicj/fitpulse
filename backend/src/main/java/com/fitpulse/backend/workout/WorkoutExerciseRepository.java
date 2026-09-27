package com.fitpulse.backend.workout;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {

    @Query("""
            SELECT we FROM WorkoutExercise we JOIN we.workout w
            WHERE w.user.id = :userId
            AND we.exercise.id = :exerciseId
            AND w.status = :status
            ORDER BY w.finishedAt DESC, we.id DESC
            """)
    List<WorkoutExercise> findLatestForUser(@Param("userId") Long userId,
                                         @Param("exerciseId") Long exerciseId,
                                         @Param("status") WorkoutStatus status,
                                         Limit limit);
}
