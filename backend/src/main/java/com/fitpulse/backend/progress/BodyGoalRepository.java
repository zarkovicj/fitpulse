package com.fitpulse.backend.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BodyGoalRepository extends JpaRepository<BodyGoal, Long> {

    Optional<BodyGoal> findByKorisnikId(Long korisnikId);
}
