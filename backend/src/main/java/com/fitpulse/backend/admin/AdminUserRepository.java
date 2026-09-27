package com.fitpulse.backend.admin;

import com.fitpulse.backend.admin.dto.AdminUserResponse;
import com.fitpulse.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// upiti samo za administraciju
// odvojeni od KorisnikRepository koji koristi ostatak aplikacije
interface AdminUserRepository extends Repository<User, Long> {

    @Query(value = """
            SELECT new com.fitpulse.backend.admin.dto.AdminUserResponse(
                u.id, u.firstName, u.lastName, u.email, u.role, u.active, u.createdAt,
                (SELECT COUNT(w) FROM Workout w
                 WHERE w.user = u AND w.status = com.fitpulse.backend.workout.WorkoutStatus.COMPLETED))
            FROM User u
            WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName, ' ', u.email)) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            ORDER BY u.createdAt DESC, u.id DESC
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName, ' ', u.email)) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            """)
    Page<AdminUserResponse> search(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT new com.fitpulse.backend.admin.dto.AdminUserResponse(
                u.id, u.firstName, u.lastName, u.email, u.role, u.active, u.createdAt,
                (SELECT COUNT(w) FROM Workout w
                 WHERE w.user = u AND w.status = com.fitpulse.backend.workout.WorkoutStatus.COMPLETED))
            FROM User u
            WHERE u.id = :id
            """)
    Optional<AdminUserResponse> findSummary(@Param("id") Long id);
}
