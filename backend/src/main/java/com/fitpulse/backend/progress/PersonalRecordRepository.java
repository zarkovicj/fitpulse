package com.fitpulse.backend.progress;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    List<PersonalRecord> findByUserIdAndExerciseIdIn(Long userId, Collection<Long> exerciseIds);

    long countByUserId(Long userId);

    @EntityGraph(attributePaths = "exercise")
    @Query("""
            SELECT r FROM PersonalRecord r
            WHERE r.user.id = :userId
            AND (:exerciseId IS NULL OR r.exercise.id = :exerciseId)
            AND (:workoutId IS NULL OR r.workoutId = :workoutId)
            ORDER BY r.achievedAt DESC, r.id
            """)
    List<PersonalRecord> search(@Param("userId") Long userId,
                             @Param("exerciseId") Long exerciseId,
                             @Param("workoutId") Long workoutId);
}
