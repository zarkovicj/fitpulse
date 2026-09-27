package com.fitpulse.backend.account;

import com.fitpulse.backend.user.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface AccountRepository extends Repository<User, Long> {

    @Query("""
            SELECT e.imageKey FROM Exercise e
            WHERE e.createdBy.id = :userId AND e.imageKey IS NOT NULL
            """)
    List<String> findImageKeysOfUser(@Param("userId") Long userId);

    // podaci koji pokazuju na vežbe korisnika brišu se pre same vežbe (i naloga)
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PersonalRecord r WHERE r.user.id = :userId")
    void deleteRecordsOfUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Workout w WHERE w.user.id = :userId")
    void deleteWorkoutsOfUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Template t WHERE t.createdBy.id = :userId")
    void deleteTemplatesOfUser(@Param("userId") Long userId);
}
