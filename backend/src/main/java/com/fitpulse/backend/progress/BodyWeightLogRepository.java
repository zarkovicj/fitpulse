package com.fitpulse.backend.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyWeightLogRepository extends JpaRepository<BodyWeightLog, Long> {

    Optional<BodyWeightLog> findByUserIdAndDate(Long userId, LocalDate date);

    Optional<BodyWeightLog> findFirstByUserIdOrderByDateDesc(Long userId);

    List<BodyWeightLog> findByUserIdAndDateBetweenOrderByDate(Long userId, LocalDate from, LocalDate to);
}
